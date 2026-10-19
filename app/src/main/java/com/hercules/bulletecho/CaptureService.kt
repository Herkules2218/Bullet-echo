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
  val step=5
  var hits=0
  var sumX=0L
  var sumY=0L
  var minX=width
  var minY=height
  var maxX=0
  var maxY=0
  for(y in 0 until height step step) {
   for(x in 0 until width step step) {
    val offset=y*row+x*pixel
    if(offset+2>=bytes.limit()) continue
    val r=bytes.get(offset).toInt() and 255
    val g=bytes.get(offset+1).toInt() and 255
    val b=bytes.get(offset+2).toInt() and 255
    if(r>175 && r>g*1.65 && r>b*1.5 && g<155) {
     hits++;sumX+=x;sumY+=y
     minX=minOf(minX,x);minY=minOf(minY,y)
     maxX=maxOf(maxX,x);maxY=maxOf(maxY,y)
    }
   }
  }
  val valid=hits>=5
  getSharedPreferences("hercules",MODE_PRIVATE).edit()
   .putInt("scan_red_hits",hits)
   .putBoolean("scan_candidate",valid)
   .putFloat("scan_x",if(valid)sumX.toFloat()/hits/width else -1f)
   .putFloat("scan_y",if(valid)sumY.toFloat()/hits/height else -1f)
   .putFloat("scan_box_w",if(valid)(maxX-minX).toFloat()/width else 0f)
   .putFloat("scan_box_h",if(valid)(maxY-minY).toFloat()/height else 0f)
   .putLong("scan_timestamp",android.os.SystemClock.elapsedRealtime())
   .apply()
 }
 override fun onDestroy() {
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
