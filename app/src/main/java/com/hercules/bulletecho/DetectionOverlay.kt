package com.hercules.bulletecho

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View

/** Visual candidate preview; not verified enemy identification. */
class DetectionOverlay(context:Context):View(context) {
 private val prefs=context.getSharedPreferences("hercules",Context.MODE_PRIVATE)
 private val frame=Paint(Paint.ANTI_ALIAS_FLAG).apply {
  color=Color.rgb(255,60,70);style=Paint.Style.STROKE;strokeWidth=3f
 }
 private val labelBg=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(155,20,35);style=Paint.Style.FILL}
 private val text=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.WHITE;textSize=25f;isFakeBoldText=true}
 private val status=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.YELLOW;textSize=22f}
 override fun onDraw(canvas:Canvas) {
  super.onDraw(canvas)
  val now=SystemClock.elapsedRealtime()
  val fresh=now-prefs.getLong("scan_timestamp",0L)<800L
  if(prefs.getBoolean("vision_boxes",false) && prefs.getBoolean("track_active",false) && fresh) {
   val x=prefs.getFloat("track_x",-1f)*width
   val y=prefs.getFloat("track_y",-1f)*height
   if(x>=0 && y>=0) {
    val halfW=(prefs.getFloat("scan_box_w",0f)*width/2f).coerceAtLeast(22f)
    val halfH=(prefs.getFloat("scan_box_h",0f)*height/2f).coerceAtLeast(28f)
    val l=(x-halfW).coerceAtLeast(0f)
    val t=(y-halfH).coerceAtLeast(0f)
    val r=(x+halfW).coerceAtMost(width.toFloat())
    val b=(y+halfH).coerceAtMost(height.toFloat())
    val corner=16f
    canvas.drawLine(l,t,l+corner,t,frame);canvas.drawLine(l,t,l,t+corner,frame)
    canvas.drawLine(r,t,r-corner,t,frame);canvas.drawLine(r,t,r,t+corner,frame)
    canvas.drawLine(l,b,l+corner,b,frame);canvas.drawLine(l,b,l,b-corner,frame)
    canvas.drawLine(r,b,r-corner,b,frame);canvas.drawLine(r,b,r,b-corner,frame)
    val label="BAR CANDIDATE"
    val tw=text.measureText(label)+16f
    val labelTop=(t-36f).coerceAtLeast(0f)
    canvas.drawRoundRect(l,labelTop,(l+tw).coerceAtMost(width.toFloat()),labelTop+33f,5f,5f,labelBg)
    canvas.drawText(label,l+8f,labelTop+25f,text)
   }
  }
  if(prefs.getBoolean("vision_debug",true)) {
  val active=prefs.getBoolean("capture_active",false)
  val frames=prefs.getLong("capture_frames",0L)
  val hits=prefs.getInt("scan_red_hits",0)
  canvas.drawText(if(active) "HERCULES | frames: $frames | hits: $hits" else "HERCULES | waiting",20f,44f,status)
  prefs.getString("scan_error",null)?.let {canvas.drawText("Scan error: $it",20f,72f,status)}
  }
  postInvalidateDelayed(180L)
 }
}
