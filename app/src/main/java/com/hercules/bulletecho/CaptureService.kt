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
  val settings=getSharedPreferences("hercules",MODE_PRIVATE)
  if(!settings.getBoolean("vision_enabled",true)) {
   settings.edit().putBoolean("track_active",false).putBoolean("scan_candidate",false).apply()
   return
  }
  val sensitivity=settings.getInt("vision_sensitivity",80).coerceIn(10,100)
  val plane=image.planes.firstOrNull() ?: return
  val bytes=plane.buffer.duplicate().order(ByteOrder.nativeOrder())
  val row=plane.rowStride
  val pixel=plane.pixelStride
  if(pixel<4) return
  val width=image.width
  val height=image.height
  // Health-bar candidate detector. Only horizontal red bars are considered;
  // arbitrary red effects and isolated pixels are deliberately ignored.
  val sensitivity=getSharedPreferences("hercules",MODE_PRIVATE).getInt("vision_sensitivity",65).coerceIn(10,100)
  val minBarSamples=(20-sensitivity/10).coerceIn(10,19)
  val step=2
  val rows=height/step
  val cols=width/step
  var hits=0
  var bestScore=-1.0
  var bestX=-1f
  var bestY=-1f
  var bestW=0f
  for(gy in 0 until rows) {
   val y=gy*step
   var runStart=-1
   var runLength=0
   fun considerRun() {
    if(runStart<0) return
    // Approximate width of the in-game health bar in half-resolution capture.
    if(runLength in minBarSamples..85) {
     val centerX=(runStart+runLength/2f)*step
     val normalizedX=centerX/width
     val normalizedY=y.toFloat()/height
     // Reject edges and the lower-center player HUD.
     if(normalizedX in 0.06f..0.94f && normalizedY in 0.08f..0.84f &&
       !(normalizedX in 0.38f..0.62f && normalizedY>0.72f)) {
      val score=runLength.toDouble()
      if(score>bestScore) {
       bestScore=score
       bestX=normalizedX
       // Sprite is generally above its health bar; estimate its center.
       bestY=((y-34f)/height).coerceIn(0f,1f)
       bestW=(runLength*step.toFloat()/width).coerceAtMost(0.15f)
      }
     }
    }
   }
   for(gx in 0 until cols) {
    val x=gx*step
    val offset=y*row+x*pixel
    if(offset+2>=bytes.limit()) continue
    val r=bytes.get(offset).toInt() and 255
    val g=bytes.get(offset+1).toInt() and 255
    val b=bytes.get(offset+2).toInt() and 255
    val red=r>190 && g<100 && b<110 && r>g*2
    if(red) {
     hits++
     if(runStart<0)runStart=gx
     runLength++
    } else {
     considerRun()
     runStart=-1
     runLength=0
    }
   }
   considerRun()
  }
  // Do not track a random red line as a confirmed enemy.
  // Requiring several consecutive frames greatly reduces jumping.
  val now=android.os.SystemClock.elapsedRealtime()
  val close=bestX>=0f && trackX>=0f &&
   kotlin.math.hypot((bestX-trackX).toDouble(),(bestY-trackY).toDouble())<0.08
  val valid=bestX>=0f && (close || !settings.getBoolean("vision_tracking",true))
  if(bestX>=0f) {
   trackX=if(close)trackX*0.7f+bestX*0.3f else bestX
   trackY=if(close)trackY*0.7f+bestY*0.3f else bestY
   if(valid) trackTime=now
  }
  val tracking=valid && now-trackTime<450L
  getSharedPreferences("hercules",MODE_PRIVATE).edit()
   .putInt("scan_red_hits",hits)
   .putBoolean("scan_candidate",valid)
   .putBoolean("track_active",tracking)
   .putFloat("track_x",if(tracking)trackX else -1f)
   .putFloat("track_y",if(tracking)trackY else -1f)
   .putFloat("scan_box_w",if(tracking)bestW else 0f)
   .putFloat("scan_box_h",if(tracking)0.11f else 0f)
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
