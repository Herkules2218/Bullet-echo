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
 private fun showPage(page:String) {
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg) }
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
   nav.addView(label(text,12f,if(page==key) gold else white,true).apply {
    gravity=Gravity.CENTER; setPadding(dp(2),dp(12),dp(2),dp(12)); setOnClickListener { showPage(key) }
   },LinearLayout.LayoutParams(0,-2,1f))
  }
  root.addView(nav,LinearLayout.LayoutParams(-1,dp(76))); setContentView(root)
  body.addView(label("HERCULES",32f,gold,true))
  body.addView(label("BULLET ECHO  •  v0.3.0",12f,violet,true))
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
    listOf("Visual settings","Notifications","Performance preferences","Custom profile").forEach { key ->
     val translated=when(key) {
      "Visual settings" -> if(english) key else "Візуальні налаштування"
      "Notifications" -> if(english) key else "Сповіщення"
      "Performance preferences" -> if(english) key else "Продуктивність"
      else -> if(english) key else "Власний профіль"
     }
     card().addView(Switch(this).apply {
      text=translated; textSize=16f; setTextColor(white); isChecked=prefs.getBoolean(key,false)
      thumbTintList=android.content.res.ColorStateList.valueOf(gold)
      trackTintList=android.content.res.ColorStateList.valueOf(Color.rgb(92,67,126))
      setOnCheckedChangeListener { _,value -> prefs.edit().putBoolean(key,value).apply() }
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
