package com.hercules.bulletecho

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import java.nio.ByteOrder
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.WindowManager
import android.view.Gravity

/**
 * v0.8: opt-in screen capture diagnostics. No input injection, no automatic aim,
 * no recording, no network access. Frame count only; image buffers are closed.
 */
class CaptureService : Service() {
 private var projection:MediaProjection?=null
 private var display:VirtualDisplay?=null
 private var reader:ImageReader?=null
 private var thread:HandlerThread?=null
 private var frames=0L
 private var lastScan=0L
 private var trackX=-1f
 private var trackY=-1f
 private var trackTime=0L
 private var diagnosticOverlay:DetectionOverlay?=null
 private val callback=object:MediaProjection.Callback() {
  override fun onStop() { stopSelf() }
 }
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(intent?.action=="stop") { stopSelf();return START_NOT_STICKY }
  val channel="hercules_capture"
  val manager=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  manager.createNotificationChannel(NotificationChannel(channel,"HERCULES Screen Analysis",NotificationManager.IMPORTANCE_LOW))
  val notification=Notification.Builder(this,channel)
   .setContentTitle("HERCULES · Screen Analysis")
   .setContentText("Аналіз кадрів увімкнено · без запису")
   .setSmallIcon(android.R.drawable.ic_menu_view)
   .setOngoing(true).build()
  if(Build.VERSION.SDK_INT>=34) startForeground(81,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
  else startForeground(81,notification)
  @Suppress("DEPRECATION")
  val token:Intent?=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra("token",Intent::class.java)
   else intent?.getParcelableExtra("token")
  if(token==null) { stopSelf();return START_NOT_STICKY }
  try {
   if(android.provider.Settings.canDrawOverlays(this)) {
    val overlay=DetectionOverlay(this)
    val params=WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
     WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
     PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START }
    (getSystemService(WINDOW_SERVICE) as WindowManager).addView(overlay,params)
    diagnosticOverlay=overlay
   }
   val mp=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
   val p=mp.getMediaProjection(Activity.RESULT_OK,token)
   projection=p
   thread=HandlerThread("HerculesCapture").apply { start() }
   val handler=Handler(thread!!.looper)
   p.registerCallback(callback,handler)
   val metrics=if(Build.VERSION.SDK_INT>=30) {
    val b=(getSystemService(WINDOW_SERVICE) as WindowManager).maximumWindowMetrics.bounds
    Pair(b.width(),b.height())
   } else {
    val d=resources.displayMetrics
    Pair(d.widthPixels,d.heightPixels)
   }
   val w=(metrics.first/2).coerceAtLeast(2)
   val h=(metrics.second/2).coerceAtLeast(2)
   reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2)
   reader!!.setOnImageAvailableListener({ source ->
    val image=source.acquireLatestImage()
    if(image!=null) {
     frames++
     val now=android.os.SystemClock.elapsedRealtime()
     if(now-lastScan>=180) {
      lastScan=now
      try { scanFrame(image) } catch(e:Exception) {
       getSharedPreferences("hercules",MODE_PRIVATE).edit().putString("scan_error",e.javaClass.simpleName).apply()
      }
     }
     if(frames%15L==0L) getSharedPreferences("hercules",MODE_PRIVATE).edit().putLong("capture_frames",frames).apply()
     image.close()
    }
   },handler)
   display=p.createVirtualDisplay("HerculesAnalysis",w,h,resources.configuration.densityDpi,
    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,handler)
   getSharedPreferences("hercules",MODE_PRIVATE).edit().putBoolean("capture_active",true).apply()
  } catch(e:Exception) {
   getSharedPreferences("hercules",MODE_PRIVATE).edit().putString("capture_error",e.javaClass.simpleName).apply()
   stopSelf()
  }
  return START_NOT_STICKY
 }
 /**
  * Fast heuristic for bright red pixel clusters. Reports a visual candidate,
  * NOT an enemy identity. RGBA planes may have padded row strides.
  */
 private fun scanFrame(image:android.media.Image) {
  val plane=image.planes.firstOrNull() ?: return
  val bytes=plane.buffer.duplicate().order(ByteOrder.nativeOrder())
  val row=plane.rowStride
  val pixel=plane.pixelStride
  if(pixel<4) return
  val width=image.width
  val height=image.height
  // Candidate segmentation: connected components rather than merging all red pixels.
  // These are color candidates, NOT identified enemy players.
  val step=4
  val cols=(width+step-1)/step
  val rows=(height+step-1)/step
  val mask=BooleanArray(cols*rows)
  var hits=0
  for(gy in 0 until rows) for(gx in 0 until cols) {
   val x=gx*step; val y=gy*step
   val offset=y*row+x*pixel
   if(offset+2>=bytes.limit()) continue
   val r=bytes.get(offset).toInt() and 255
   val g=bytes.get(offset+1).toInt() and 255
   val b=bytes.get(offset+2).toInt() and 255
   if(r>175 && r>g*1.65 && r>b*1.5 && g<155) {
    mask[gy*cols+gx]=true
    hits++
   }
  }
  val seen=BooleanArray(mask.size)
  val queue=IntArray(mask.size)
  var bestScore=Double.NEGATIVE_INFINITY
  var bestX=-1f;var bestY=-1f;var bestW=0f;var bestH=0f
  for(i in mask.indices) {
   if(!mask[i] || seen[i]) continue
   var head=0;var tail=1
   queue[0]=i;seen[i]=true
   var count=0;var minX=cols;var minY=rows;var maxX=0;var maxY=0
   var sumX=0L;var sumY=0L
   while(head<tail) {
    val v=queue[head++];val x=v%cols;val y=v/cols
    count++;sumX+=x;sumY+=y
    minX=minOf(minX,x);minY=minOf(minY,y)
    maxX=maxOf(maxX,x);maxY=maxOf(maxY,y)
    val neighbors=intArrayOf(v-1,v+1,v-cols,v+cols)
    for(n in neighbors) {
     if(n<0 || n>=mask.size || !mask[n] || seen[n]) continue
     if((n==v-1 || n==v+1) && n/cols!=y) continue
     seen[n]=true;queue[tail++]=n
    }
   }
   val bw=(maxX-minX+1f)/cols
   val bh=(maxY-minY+1f)/rows
   if(count<3 || count>180 || bw>0.13f || bh>0.22f) continue
   val cx=(sumX.toFloat()/count+0.5f)/cols
   val cy=(sumY.toFloat()/count+0.5f)/rows
   // Exclude central aiming cone and player's sprite region.
   if(cx in 0.38f..0.62f && cy in 0.30f..0.80f) continue
   val near=if(trackX>=0f) kotlin.math.hypot((cx-trackX).toDouble(),(cy-trackY).toDouble()) else 1.0
   val score=count.toDouble()-near*20.0
   if(score>bestScore) {
    bestScore=score;bestX=cx;bestY=cy;bestW=bw;bestH=bh
   }
  }
  val valid=bestX>=0f
  val now=android.os.SystemClock.elapsedRealtime()
  val close=valid && trackX>=0f && kotlin.math.hypot((bestX-trackX).toDouble(),(bestY-trackY).toDouble())<0.16
  if(valid) {
   trackX=if(close)trackX*0.6f+bestX*0.4f else bestX
   trackY=if(close)trackY*0.6f+bestY*0.4f else bestY
   trackTime=now
  }
  val tracking=trackX>=0f && now-trackTime<600L
  getSharedPreferences("hercules",MODE_PRIVATE).edit()
   .putInt("scan_red_hits",hits)
   .putBoolean("scan_candidate",valid)
   .putBoolean("track_active",tracking)
   .putFloat("track_x",if(tracking)trackX else -1f)
   .putFloat("track_y",if(tracking)trackY else -1f)
   .putFloat("scan_box_w",if(valid)bestW else 0f)
   .putFloat("scan_box_h",if(valid)bestH else 0f)
   .putLong("scan_timestamp",now)
   .apply()
 }
 override fun onDestroy() {
  diagnosticOverlay?.let { view ->
   try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view) } catch(_:Exception) {}
  }
  diagnosticOverlay=null
  getSharedPreferences("hercules",MODE_PRIVATE).edit().putBoolean("capture_active",false).apply()
  reader?.setOnImageAvailableListener(null,null)
  display?.release()
  reader?.close()
  projection?.unregisterCallback(callback)
  projection?.stop()
  thread?.quitSafely()
  super.onDestroy()
 }
}
