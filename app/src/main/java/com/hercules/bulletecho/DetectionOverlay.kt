package com.hercules.bulletecho

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View

/** Non-interactive diagnostic overlay. Shows red-color candidate bounds only. */
class DetectionOverlay(context:Context):View(context) {
 private val prefs=context.getSharedPreferences("hercules",Context.MODE_PRIVATE)
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.YELLOW;style=Paint.Style.STROKE;strokeWidth=3f }
 private val textPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE;textSize=32f }
 override fun onDraw(canvas:Canvas) {
  super.onDraw(canvas)
  if(prefs.getBoolean("scan_candidate",false) &&
    SystemClock.elapsedRealtime()-prefs.getLong("scan_timestamp",0L)<1200L) {
   val x=prefs.getFloat("scan_x",-1f)*width
   val y=prefs.getFloat("scan_y",-1f)*height
   val halfW=(prefs.getFloat("scan_box_w",0f)*width/2f).coerceAtLeast(16f)
   val halfH=(prefs.getFloat("scan_box_h",0f)*height/2f).coerceAtLeast(16f)
   canvas.drawRect(x-halfW,y-halfH,x+halfW,y+halfH,paint)
   canvas.drawText("RED CANDIDATE", (x-halfW).coerceAtLeast(0f),(y-halfH-10f).coerceAtLeast(35f),textPaint)
  }
  postInvalidateDelayed(180L)
 }
}
