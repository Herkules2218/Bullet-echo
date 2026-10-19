package com.hercules.bulletecho

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
 private val bg = Color.rgb(13, 9, 24)
 private val panel = Color.rgb(34, 24, 54)
 private val gold = Color.rgb(235, 186, 83)
 private val violet = Color.rgb(181, 133, 244)
 private val white = Color.rgb(244, 239, 255)
 private val prefs by lazy { getSharedPreferences("hercules", MODE_PRIVATE) }
 private lateinit var body: LinearLayout
 private var english = false
 private fun dp(v:Int) = (v*resources.displayMetrics.density).toInt()
 private fun shape(color:Int) = GradientDrawable().apply { setColor(color); cornerRadius=dp(18).toFloat() }
 private fun label(s:String, size:Float, color:Int=white, bold:Boolean=false) = TextView(this).apply {
  text=s; textSize=size; setTextColor(color); if(bold) typeface=Typeface.DEFAULT_BOLD
 }
 private fun heading(s:String) { body.addView(label(s,22f,white,true)) }
 private fun card():LinearLayout {
  val c=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(16),dp(18),dp(16)); background=shape(panel) }
  body.addView(c,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(14) })
  return c
 }
 private fun action(s:String, f:()->Unit) {
  val b=Button(this).apply { text=s; isAllCaps=false; setTextColor(white); background=shape(Color.rgb(81,46,124)); setOnClickListener { f() } }
  body.addView(b,LinearLayout.LayoutParams(-1,dp(56)).apply { topMargin=dp(14) })
 }
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor=bg; window.navigationBarColor=bg
  window.decorView.systemUiVisibility=0
  window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
  english=prefs.getBoolean("english",false)
  showPage("home")
 }
 private fun overlay(action:String) {
  if(!android.provider.Settings.canDrawOverlays(this)) {
   startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
    android.net.Uri.parse("package:"+packageName)))
   Toast.makeText(this,"Дозволь відображення поверх інших застосунків і повернись",Toast.LENGTH_LONG).show()
   return
  }
  startService(android.content.Intent(this,OverlayService::class.java).apply { this.action=action })
 }
 private fun showPage(page:String) {
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg) }
  if (android.os.Build.VERSION.SDK_INT >= 30) {
   root.setOnApplyWindowInsetsListener { view, insets ->
    val bars=insets.getInsets(android.view.WindowInsets.Type.navigationBars())
    view.setPadding(0,0,0,bars.bottom)
    insets
   }
  } else {
   @Suppress("DEPRECATION")
   val navId=resources.getIdentifier("navigation_bar_height","dimen","android")
   if(navId>0) root.setPadding(0,0,0,resources.getDimensionPixelSize(navId))
  }
  val scroll=ScrollView(this)
  body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(48),dp(20),dp(30)) }
  scroll.addView(body)
  root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val nav=LinearLayout(this).apply { setBackgroundColor(panel); gravity=Gravity.CENTER; setPadding(0,dp(8),0,dp(12)) }
  listOf("home","features","profiles","settings").forEach { key ->
   val text=when(key) {
    "home" -> if(english) "Home" else "Головна"
    "features" -> if(english) "Features" else "Функції"
    "profiles" -> if(english) "Profiles" else "Профілі"
    else -> if(english) "Settings" else "Опції"
   }
   val symbol=when(key) { "home" -> "⌂"; "features" -> "☷"; "profiles" -> "♙"; else -> "⚙" }
   nav.addView(label("$symbol  $text",12f,if(page==key) gold else white,true).apply {
    gravity=Gravity.CENTER; setPadding(dp(2),dp(12),dp(2),dp(12)); setOnClickListener { showPage(key) }
   },LinearLayout.LayoutParams(0,-2,1f))
  }
  root.addView(nav,LinearLayout.LayoutParams(-1,dp(76))); setContentView(root)
  body.addView(label("HERCULES",32f,gold,true))
  body.addView(label("BULLET ECHO  •  v0.6.0",12f,violet,true))
  body.addView(Space(this),LinearLayout.LayoutParams(1,dp(26)))
  when(page) {
   "home" -> {
    heading(if(english) "CONTROL CENTER" else "ЦЕНТР КЕРУВАННЯ")
    val c=card()
    c.addView(label(if(english) "SYSTEM READY" else "СИСТЕМА ГОТОВА",18f,gold,true))
    c.addView(label(if(english) "Standalone menu. Game integration is not enabled." else "Окреме меню. Інтеграція з грою поки не активна.",14f))
    action(if(english) "Open features" else "Відкрити функції") { showPage("features") }
   }
   "features" -> {
    heading(if(english) "FEATURES" else "ФУНКЦІЇ")
    val crossCard=card()
    crossCard.addView(label("🎯 Custom Crosshair",18f,gold,true))
    crossCard.addView(label(if(english) "Crosshair over other apps" else "Приціл поверх інших застосунків",14f))
    action(if(english) "Enable crosshair" else "Увімкнути приціл") { overlay("cross_on") }
    action(if(english) "Disable crosshair" else "Вимкнути приціл") { overlay("cross_off") }
    val aimCard=card()
    aimCard.addView(label("🎯 Aim Assist — Research",18f,gold,true))
    aimCard.addView(label(if(english) "Target tracking is not active. Requires separate technical development." else "Автоматичне наведення поки не реалізоване. Потрібна окрема розробка.",13f))
    val bubbleCard=card()
    bubbleCard.addView(label("💜 HERCULES Floating Menu",18f,gold,true))
    action(if(english) "Show floating H" else "Показати плаваючу H") { overlay("bubble_on") }
    action(if(english) "Hide floating H" else "Сховати плаваючу H") { overlay("bubble_off") }
    val settingsCard=card()
    settingsCard.addView(label(if(english) "Crosshair options" else "Налаштування прицілу",17f,gold,true))
    settingsCard.addView(Switch(this).apply {
     text=if(english) "Dot instead of cross" else "Точка замість хрестика";setTextColor(white)
     isChecked=prefs.getBoolean("cross_dot",false)
     setOnCheckedChangeListener { _,v -> prefs.edit().putBoolean("cross_dot",v).apply();overlay("refresh") }
    })
    settingsCard.addView(label(if(english) "Opacity" else "Прозорість",14f))
    settingsCard.addView(SeekBar(this).apply {
     max=100;progress=prefs.getInt("cross_alpha",90)
     setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean) { if(user) prefs.edit().putInt("cross_alpha",v.coerceAtLeast(10)).apply() }
      override fun onStartTrackingTouch(s:SeekBar?) {}
      override fun onStopTrackingTouch(s:SeekBar?) { overlay("refresh") }
     })
    })
    listOf("Visual settings","Notifications","Performance preferences","Custom profile").forEach { key ->
     val translated=when(key) {
      "Visual settings" -> if(english) key else "Візуальні налаштування"
      "Notifications" -> if(english) key else "Сповіщення"
      "Performance preferences" -> if(english) key else "Продуктивність"
      else -> if(english) key else "Власний профіль"
     }
     card().addView(Switch(this).apply {
      text=translated; textSize=16f; setTextColor(white); isChecked=prefs.getBoolean(prefs.getString("profile","Default")+"_"+key,false)
      thumbTintList=android.content.res.ColorStateList(
       arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
       intArrayOf(gold,Color.rgb(160,154,174)))
      trackTintList=android.content.res.ColorStateList(
       arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
       intArrayOf(Color.rgb(112,66,156),Color.rgb(68,62,81)))
      setOnCheckedChangeListener { _,value -> prefs.edit().putBoolean(prefs.getString("profile","Default")+"_"+key,value).apply() }
     })
    }
    body.addView(label(if(english) "Switches store preferences only." else "Перемикачі поки лише зберігають налаштування.",12f,violet))
   }
   "profiles" -> {
    heading(if(english) "PROFILES" else "ПРОФІЛІ")
    val c=card()
    listOf("Default","Custom 1","Custom 2").forEach { name ->
     c.addView(Button(this).apply { text=name; isAllCaps=false; setTextColor(white); background=shape(if(prefs.getString("profile","Default")==name) Color.rgb(111,65,155) else Color.rgb(68,42,103)); setOnClickListener {
      prefs.edit().putString("profile",name).apply(); showPage("profiles")
     } })
    }
    body.addView(label((if(english) "Selected: " else "Обрано: ")+prefs.getString("profile","Default"),14f,violet))
   }
   else -> {
    heading(if(english) "SETTINGS" else "НАЛАШТУВАННЯ")
    card().addView(Switch(this).apply {
     text=if(english) "English" else "English / Українська"; setTextColor(white); textSize=16f; isChecked=english
     thumbTintList=android.content.res.ColorStateList.valueOf(gold)
     setOnCheckedChangeListener { _,value ->
      english=value; prefs.edit().putBoolean("english",value).apply(); showPage("settings")
     }
    })
    body.addView(label(if(english) "Dark purple and gold theme" else "Темна фіолетово-золота тема",14f,violet))
   }
  }
 }
}
