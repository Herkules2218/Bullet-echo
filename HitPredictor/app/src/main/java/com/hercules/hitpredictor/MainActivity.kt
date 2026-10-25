package com.hercules.hitpredictor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(35,65,35,25) }
  val title = TextView(this).apply { text="HERCULES HIT PREDICTOR — Prototype"; textSize=22f }
  val info = TextView(this).apply { text="Прототип прицілу поверх гри. Колір змінюється вручну; реальний прогноз попадання поки не реалізовано. Натисни на приціл для зміни кольору, перетягни для переміщення."; textSize=16f }
  val start = Button(this).apply { text="Увімкнути приціл"; setOnClickListener {
   if (!Settings.canDrawOverlays(this@MainActivity)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
   else startService(Intent(this@MainActivity, OverlayService::class.java).setAction("START"))
  } }
  val stop = Button(this).apply { text="Вимкнути"; setOnClickListener { stopService(Intent(this@MainActivity, OverlayService::class.java)) } }
  root.addView(title);root.addView(info);root.addView(start);root.addView(stop);setContentView(root)
 }
}