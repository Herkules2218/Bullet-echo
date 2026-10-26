package com.hercules.hitpredictor

import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.*

class OverlayService : Service() {
 private lateinit var wm:WindowManager
 private var overlay:View?=null
 private var params:WindowManager.LayoutParams?=null
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(overlay!=null){stopSelf();return START_NOT_STICKY}
  val range=intent?.getDoubleExtra("range",103.0)?:103.0
  val angle=Math.toRadians(intent?.getDoubleExtra("angle",30.0)?:30.0)
  val speed=(intent?.getDoubleExtra("speed",90.0)?:90.0).coerceAtLeast(1.0)
  val gravity=32.174
  val time=range/(speed*cos(angle)).coerceAtLeast(0.01)
  val impactHeight=range*tan(angle)-0.5*gravity*time*time
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  val view=object:View(this){
   val paint=Paint(Paint.ANTI_ALIAS_FLAG)
   var lastX=0f;var lastY=0f
   override fun onDraw(c:Canvas){
    super.onDraw(c)
    val cx=width/2f;val cy=height/2f
    paint.color=Color.RED;paint.style=Paint.Style.STROKE;paint.strokeWidth=4f
    c.drawCircle(cx,cy,19f,paint);c.drawLine(cx-30,cy,cx+30,cy,paint);c.drawLine(cx,cy-30,cx,cy+30,paint)
    paint.style=Paint.Style.FILL;paint.textSize=22f;paint.color=Color.WHITE
    c.drawText("MODEL: %.1f ft".format(impactHeight),12f,35f,paint)
    paint.textSize=16f;c.drawText("Drag to align",12f,55f,paint)
   }
   override fun onTouchEvent(e:MotionEvent):Boolean{
    when(e.actionMasked){
     MotionEvent.ACTION_DOWN->{lastX=e.rawX;lastY=e.rawY;return true}
     MotionEvent.ACTION_MOVE->{val p=params?:return true;p.x+=(e.rawX-lastX).toInt();p.y+=(e.rawY-lastY).toInt();lastX=e.rawX;lastY=e.rawY;wm.updateViewLayout(this,p);return true}
    };return true
   }
  }
  val p=WindowManager.LayoutParams(250,130,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,android.graphics.PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=200;y=400}
  params=p;overlay=view;wm.addView(view,p)
  return START_NOT_STICKY
 }
 override fun onDestroy(){overlay?.let{wm.removeView(it)};overlay=null;super.onDestroy()}
}