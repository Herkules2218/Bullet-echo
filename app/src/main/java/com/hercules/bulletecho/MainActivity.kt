package com.hercules.bulletecho

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.widget.*

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val prefs = getSharedPreferences("hercules", MODE_PRIVATE)
  val root = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(32, 48, 32, 32)
   setBackgroundColor(Color.rgb(13, 10, 22))
  }
  fun title(value: String, size: Float, color: Int) = TextView(this).apply {
   text=value; textSize=size; setTextColor(color); typeface=Typeface.DEFAULT_BOLD
  }
  root.addView(title("HERCULES", 32f, Color.rgb(221, 178, 72)))
  root.addView(title("BULLET ECHO • CONTROL CENTER", 12f, Color.rgb(175, 140, 235)))
  val features = listOf("Visual settings", "Notifications", "Performance preferences", "Custom profile")
  features.forEach { name ->
   val toggle = Switch(this).apply {
    text=name; textSize=17f; setTextColor(Color.WHITE)
    setPadding(14, 25, 14, 25)
    isChecked=prefs.getBoolean(name, false)
    setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(name, checked).apply() }
   }
   root.addView(toggle)
  }
  root.addView(title("Settings are saved locally. Game integration is not enabled.", 12f, Color.LTGRAY))
  val scroll=ScrollView(this); scroll.addView(root); setContentView(scroll)
 }
}
