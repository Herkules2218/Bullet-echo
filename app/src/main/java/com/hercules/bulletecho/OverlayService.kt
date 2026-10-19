package com.hercules.bulletecho

import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.os.Handler
import android.os.Looper

class OverlayService: Service() {
 private lateinit var wm:WindowManager
 private var cross:View?=null
 private var timer:TextView?=null
 private val handler=Handler(Looper.getMainLooper())
 private var started=0L
 private val tick=object:Runnable {
  override fun run() {
   timer?.text=String.format(java.util.Locale.US,"⏱ %02d:%02d",(SystemClock.elapsedRealtime()-started)/60000,(SystemClock.elapsedRealtime()-started)/1000%60)
   if(timer!=null) handler.postDelayed(this,500)
  }
 }
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  when(intent?.action) {
   "cross_on" -> showCross()
   "cross_off" -> { cross?.let { wm.removeView(it) };cross=null }
   "timer_on" -> showTimer()
   "timer_off" -> { timer?.let { wm.removeView(it) };timer=null;handler.removeCallbacks(tick) }
   "timer_reset" -> { started=SystemClock.elapsedRealtime() }
   "refresh" -> { if(cross!=null) showCross() }
  }
  if(cross==null && timer==null) stopSelf()
  return START_STICKY
 }
 private fun params(w:Int,h:Int,gravity:Int)=WindowManager.LayoutParams(w,h,
  WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
  android.graphics.PixelFormat.TRANSLUCENT).apply { this.gravity=gravity }
 private fun showCross() {
  cross?.let { wm.removeView(it) }
  val p=getSharedPreferences("hercules",MODE_PRIVATE)
  val size=p.getInt("cross_size",26)
  val color=when(p.getInt("cross_color",0)) { 1->Color.WHITE;2->Color.RED;3->Color.GREEN;else->Color.rgb(235,186,83) }
  val alpha=p.getInt("cross_alpha",90)
  val dot=p.getBoolean("cross_dot",false)
  cross=object:View(this) {
   private val paint=Paint(3).apply { this.color=color; this.alpha=alpha*255/100; strokeWidth=3f*resources.displayMetrics.density; strokeCap=Paint.Cap.ROUND }
   override fun onDraw(c:Canvas) {
    val x=width/2f;val y=height/2f
    if(dot) c.drawCircle(x,y,3.5f*resources.displayMetrics.density,paint)
    else {
     val r=size*resources.displayMetrics.density/2
     val gap=4*resources.displayMetrics.density
     c.drawLine(x-r,y,x-gap,y,paint);c.drawLine(x+gap,y,x+r,y,paint)
     c.drawLine(x,y-r,x,y-gap,paint);c.drawLine(x,y+gap,x,y+r,paint)
    }
   }
  }
  wm.addView(cross,params(-1,-1,Gravity.CENTER))
 }
 private fun showTimer() {
  if(timer!=null) return
  started=SystemClock.elapsedRealtime()
  timer=TextView(this).apply {
   textSize=19f;setTextColor(Color.rgb(235,186,83))
   setShadowLayer(5f,1f,2f,Color.BLACK)
   setPadding(16,12,16,12)
  }
  wm.addView(timer,params(-2,-2,Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { y=(75*resources.displayMetrics.density).toInt() })
  handler.post(tick)
 }
 override fun onDestroy() {
  handler.removeCallbacks(tick)
  cross?.let { wm.removeView(it) };timer?.let { wm.removeView(it) }
  super.onDestroy()
 }
}
