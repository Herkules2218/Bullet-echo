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
import kotlin.math.abs

class OverlayService : Service() {
 private lateinit var wm: WindowManager
 private var overlay: View? = null
 private var params: WindowManager.LayoutParams? = null
 override fun onBind(intent: Intent?): IBinder? = null
 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  if (overlay != null) return START_STICKY
  wm = getSystemService(WINDOW_SERVICE) as WindowManager
  val view = object : View(this) {
   val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeWidth=5f }
   var state=0
   override fun onDraw(canvas: Canvas) { super.onDraw(canvas)
    paint.color = intArrayOf(Color.GREEN,Color.YELLOW,Color.RED)[state]
    val x=width/2f; val y=height/2f
    canvas.drawCircle(x,y,32f,paint)
    canvas.drawLine(x-49,y,x-39,y,paint);canvas.drawLine(x+39,y,x+49,y,paint)
    canvas.drawLine(x,y-49,x,y-39,paint);canvas.drawLine(x,y+39,x,y+49,paint)
   }
   var downX=0f;var downY=0f;var lastX=0f;var lastY=0f
   override fun onTouchEvent(event: MotionEvent): Boolean { when(event.actionMasked) {
    MotionEvent.ACTION_DOWN -> {downX=event.rawX;downY=event.rawY;lastX=downX;lastY=downY;return true}
    MotionEvent.ACTION_MOVE -> { val p=params ?: return true; p.x+=(event.rawX-lastX).toInt();p.y+=(event.rawY-lastY).toInt();lastX=event.rawX;lastY=event.rawY;wm.updateViewLayout(this,p);return true }
    MotionEvent.ACTION_UP -> {if(abs(event.rawX-downX)<15 && abs(event.rawY-downY)<15){state=(state+1)%3;invalidate()};return true}
   };return true }
  }
  val p=WindowManager.LayoutParams(120,120,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
   android.graphics.PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START;x=250;y=400 }
  params=p;overlay=view;wm.addView(view,p)
  return START_STICKY
 }
 override fun onDestroy() { overlay?.let { wm.removeView(it) };overlay=null;super.onDestroy() }
}