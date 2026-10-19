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
 private var bubble:TextView?=null
 private var menu:android.widget.LinearLayout?=null
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  when(intent?.action) {
   "cross_on" -> showCross()
   "cross_off" -> { cross?.let { wm.removeView(it) };cross=null }
   "bubble_on" -> showBubble()
   "bubble_off" -> hideBubble()

   "refresh" -> { if(cross!=null) showCross() }
  }
  if(cross==null && bubble==null) stopSelf()
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
 private fun hideBubble() {
  menu?.let { wm.removeView(it) };menu=null
  bubble?.let { wm.removeView(it) };bubble=null
 }
 private fun showBubble() {
  if(bubble!=null) return
  bubble=TextView(this).apply {
   text="H";textSize=22f;setTextColor(Color.rgb(235,186,83));gravity=Gravity.CENTER
   background=android.graphics.drawable.GradientDrawable().apply {
    setColor(Color.rgb(57,29,90));cornerRadius=100f
   }
   setOnClickListener { toggleMenu() }
  }
  wm.addView(bubble,params((54*resources.displayMetrics.density).toInt(),(54*resources.displayMetrics.density).toInt(),Gravity.END or Gravity.CENTER_VERTICAL).apply {
   flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
   x=12
  })
 }
 private fun toggleMenu() {
  if(menu!=null) { menu?.let { wm.removeView(it) };menu=null;return }
  val p=getSharedPreferences("hercules",MODE_PRIVATE)
  val layout=android.widget.LinearLayout(this).apply {
   orientation=android.widget.LinearLayout.VERTICAL
   setPadding(20,20,20,20)
   background=android.graphics.drawable.GradientDrawable().apply {
    setColor(Color.rgb(34,24,54));cornerRadius=28f
   }
  }
  fun item(title:String,action:()->Unit) {
   layout.addView(TextView(this).apply {
    text=title;textSize=17f;setTextColor(Color.WHITE);setPadding(16,16,16,16)
    setOnClickListener { action() }
   })
  }
  item("🎯 "+if(cross==null)"Увімкнути приціл" else "Вимкнути приціл") {
   if(cross==null) showCross() else { cross?.let { wm.removeView(it) };cross=null }
   menu?.let { wm.removeView(it) };menu=null
  }
  item("🎨 Змінити колір") {
   p.edit().putInt("cross_color",(p.getInt("cross_color",0)+1)%4).apply()
   if(cross!=null) showCross()
  }
  item("＋ Збільшити приціл") {
   p.edit().putInt("cross_size",(p.getInt("cross_size",26)+4).coerceAtMost(64)).apply()
   if(cross!=null) showCross()
  }
  item("－ Зменшити приціл") {
   p.edit().putInt("cross_size",(p.getInt("cross_size",26)-4).coerceAtLeast(8)).apply()
   if(cross!=null) showCross()
  }
  item("✕ Закрити меню") { menu?.let { wm.removeView(it) };menu=null }
  menu=layout
  wm.addView(layout,params((235*resources.displayMetrics.density).toInt(),-2,Gravity.END or Gravity.CENTER_VERTICAL).apply {
   flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
   x=(72*resources.displayMetrics.density).toInt()
  })
 }
 override fun onDestroy() {
  cross?.let { wm.removeView(it) };hideBubble()
  super.onDestroy()
 }
}
