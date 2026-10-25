package com.hercules.hitpredictor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30,50,30,20) }
  val title = TextView(this).apply { text="HERCULES HIT PREDICTOR v0.2 — калібрування"; textSize=21f }
  val info = TextView(this).apply { text="Тестова балістична модель. Вкажи відстань і кут гармати. Червона точка показує розраховану позицію на площині цілі. Автоматичне розпізнавання ворогів та реальна фізика Blast Busters ще НЕ реалізовані.";textSize=15f }
  val rangeLabel=TextView(this).apply { text="Відстань: 103 ft" }
  val range=SeekBar(this).apply { max=300;progress=103 }
  val angleLabel=TextView(this).apply { text="Кут гармати: 30°" }
  val angle=SeekBar(this).apply { max=80;progress=30 }
  val speedLabel=TextView(this).apply { text="Швидкість: 90 ft/s (тестова)" }
  val speed=SeekBar(this).apply { max=180;progress=90 }
  fun update() {rangeLabel.text="Відстань: ${range.progress} ft";angleLabel.text="Кут гармати: ${angle.progress}°";speedLabel.text="Швидкість: ${speed.progress} ft/s (тестова)"}
  range.setOnSeekBarChangeListener(listener {update()});angle.setOnSeekBarChangeListener(listener {update()});speed.setOnSeekBarChangeListener(listener {update()})
  val start=Button(this).apply {text="Показати прогноз поверх гри";setOnClickListener {
   if (!Settings.canDrawOverlays(this@MainActivity)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
   else startService(Intent(this@MainActivity,OverlayService::class.java).apply {putExtra("range",range.progress.toDouble());putExtra("angle",angle.progress.toDouble());putExtra("speed",speed.progress.toDouble())})
  }}
  val stop=Button(this).apply {text="Прибрати маркер";setOnClickListener {stopService(Intent(this@MainActivity,OverlayService::class.java))}}
  listOf(title,info,rangeLabel,range,angleLabel,angle,speedLabel,speed,start,stop).forEach {root.addView(it)}
  setContentView(root)
 }
 private fun listener(f:()->Unit)=object:SeekBar.OnSeekBarChangeListener {
  override fun onProgressChanged(s:SeekBar?,p:Int,fromUser:Boolean){f()}
  override fun onStartTrackingTouch(s:SeekBar?){}
  override fun onStopTrackingTouch(s:SeekBar?){}
 }
}