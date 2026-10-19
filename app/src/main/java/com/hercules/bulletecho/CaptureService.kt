package com.hercules.bulletecho

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
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
  val token=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra("token",Intent::class.java)
   else @Suppress("DEPRECATION") intent?.getParcelableExtra<Intent>("token")
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
    @Suppress("DEPRECATION") val d=resources.displayMetrics
    Pair(d.widthPixels,d.heightPixels)
   }
   val w=(metrics.first/2).coerceAtLeast(2)
   val h=(metrics.second/2).coerceAtLeast(2)
   reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2)
   reader!!.setOnImageAvailableListener({ source ->
    val image=source.acquireLatestImage()
    if(image!=null) {
     frames++
     getSharedPreferences("hercules",MODE_PRIVATE).edit().putLong("capture_frames",frames).apply()
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
