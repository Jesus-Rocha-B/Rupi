package pe.rupi.mobile

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/** Native Android views; it does not embed the web app in a WebView. */
class MainActivity : Activity() {
 companion object { private var api: RupiApi? = null }
 private lateinit var layout: LinearLayout
 private lateinit var notice: TextView
 private val worker = Executors.newSingleThreadExecutor()
 private var busy = false
 private var alive = true
 private var screen = 0
 private var summaryMode=false
 private var cardMode=false
 private var currentTopic: JSONObject? = null
 private var voice: RupiVoice? = null
 private val challengeAnswers = mutableMapOf<String,JSONObject>()
 /** Ruta y parada abiertas: se guardan para volver al mismo lugar si Android recrea la pantalla. */
 private var currentRoute: JSONObject? = null
 private var currentNode: JSONObject? = null
 /** Respuestas de la lección abierta (HU-02), como en la web: se muestran hasta salir de la parada. */
 private val lessonAnswers = mutableMapOf<String,JSONObject>()
 private var lessonNodeId = ""
 private var challengeDay = ""
 /** Pantalla a la que vuelve «Atrás»; null en el ingreso y en «Mis temas», donde se sale de la app. */
 private var onBack: (() -> Unit)? = null
 private lateinit var root: ScrollView
 /** Al volver a dibujar la misma pantalla (p. ej., tras responder) se regresa a la misma altura. */
 private var pendingScroll = 0
 private var restoreScroll = 0
 private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

 private val summaryConnectionReceiver=object:android.content.BroadcastReceiver(){
  override fun onReceive(context:android.content.Context?,intent:android.content.Intent?){
   val connected=(getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).activeNetworkInfo?.isConnected==true
   val topic=currentTopic
   if(connected && alive && !busy && summaryMode && api!=null && topic!=null)loadSummary(topic)
  }
 }

 override fun onCreate(state: Bundle?) {
  super.onCreate(state)
  // Barra de estado sobre la tinta de bosque (íconos claros) y navegación sobre el papel (íconos oscuros).
  @Suppress("DEPRECATION") window.statusBarColor = RupiUi.FOREST
  @Suppress("DEPRECATION") window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  voice=RupiVoice(this)
  registerReceiver(summaryConnectionReceiver,android.content.IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION))
  if(android.os.Build.VERSION.SDK_INT>=33) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { goBack() }
  if(api==null)api=StudyAlerts.restore(this)
  if (api == null) loginScreen() else {
   val topic = state?.getString("topic")
   val route = state?.getString("route")?.let { JSONObject(it) }
   val node = state?.getString("node")?.let { JSONObject(it) }
   if (topic == null && route != null) { if(node!=null) lessonScreen(route,node) else routeScreen(route) }
   else if (topic != null) { if(state?.getBoolean("summary")==true) loadSummary(JSONObject(topic)) else if(state?.getBoolean("cards")==true) loadCards(JSONObject(topic)) else loadTopic(JSONObject(topic)) } else if(intent.getBooleanExtra("notifications",false))notifications() else topics()
  }
 }
 override fun onSaveInstanceState(out: Bundle) { currentTopic?.let { out.putString("topic", it.toString()) }
  currentRoute?.let { out.putString("route", it.toString()) }; currentNode?.let { out.putString("node", it.toString()) }; out.putBoolean("summary",summaryMode);out.putBoolean("cards",cardMode); super.onSaveInstanceState(out) }
 override fun onDestroy() { voice?.shutdown();unregisterReceiver(summaryConnectionReceiver);alive = false; worker.shutdownNow(); super.onDestroy() }
 @Deprecated("Android 13+ usa OnBackInvokedDispatcher") override fun onBackPressed() { goBack() }
 /** Sin esto, «Atrás» cerraba la app desde cualquier pantalla porque todas viven en una sola Activity. */
 private fun goBack() {
  val back=onBack
  if(back==null) { finish(); return }
  if(!busy) back()
 }
 /** Barra superior de la consola: marca «R», «Rupi» y el nombre de la pantalla, sobre tinta de bosque. */
 private fun header(title: String): LinearLayout {
  val bar = LinearLayout(this).apply {
   orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL
   background=RupiUi.topbar(this@MainActivity)
  }
  bar.addView(TextView(this).apply {
   text="R"; typeface=RupiUi.TITLE; textSize=22f; setTextColor(Color.WHITE); gravity=android.view.Gravity.CENTER
   background=RupiUi.brandMark(this@MainActivity); setPadding(0,0,0,dp(4)); importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }, LinearLayout.LayoutParams(dp(44),dp(48)))
  val names = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),0,0,0) }
  names.addView(TextView(this).apply { text="Rupi"; typeface=RupiUi.TITLE; textSize=20f; setTextColor(Color.WHITE) })
  names.addView(TextView(this).apply {
   text=title; typeface=RupiUi.BOLD; textSize=14f; setTextColor(Color.rgb(214,232,220))
   if(android.os.Build.VERSION.SDK_INT>=28) isAccessibilityHeading=true
  })
  bar.addView(names, LinearLayout.LayoutParams(0,-2,1f))
  return bar
 }
 private fun page(title: String, back: (() -> Unit)? = null) {
  screen++
  onBack=back
  restoreScroll=pendingScroll;pendingScroll=0
  voice?.stop()
  val frame = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=RupiUi.Paper(this@MainActivity) }
  val bar = header(title)
  bar.setPadding(dp(16),dp(12),dp(16),dp(16))
  frame.addView(bar, LinearLayout.LayoutParams(-1,-2))
  val scroll = ScrollView(this)
  root=scroll
  scroll.isFillViewport = true
  scroll.clipToPadding = false
  layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16),dp(8),dp(16),dp(24)) }
  scroll.addView(layout, android.widget.FrameLayout.LayoutParams(-1,-2))
  frame.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
  setContentView(frame)
  // Pantalla completa (obligatoria desde Android 15): la barra verde ocupa la zona de la hora y el contenido no
  // queda bajo los botones de navegación.
  frame.setOnApplyWindowInsetsListener { _,insets ->
   @Suppress("DEPRECATION") val top=insets.systemWindowInsetTop
   @Suppress("DEPRECATION") val bottom=insets.systemWindowInsetBottom
   bar.setPadding(dp(16),top+dp(12),dp(16),dp(16))
   layout.setPadding(dp(16),dp(8),dp(16),dp(24)+bottom)
   insets
  }
  notice = TextView(this).apply {
   typeface=RupiUi.BOLD; textSize=16f; setPadding(dp(14),dp(12),dp(14),dp(12)); visibility=View.GONE
   accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
   addTextChangedListener(object: android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: android.text.Editable?) { paintNotice() }
   })
  }
  layout.addView(notice, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8); bottomMargin=dp(4) })
 }
 /** Mensaje arriba de la pantalla con los colores de la consola: información, error o logro. */
 private var noticeTone = RupiUi.Tone.INFO
 private fun paintNotice() {
  notice.visibility = if(notice.text.isNullOrEmpty()) View.GONE else View.VISIBLE
  notice.background = RupiUi.rounded(noticeTone.back, dp(14).toFloat(), dp(2), noticeTone.border)
  notice.setTextColor(noticeTone.text)
 }
 private fun setNotice(message: String, tone: RupiUi.Tone = RupiUi.Tone.INFO) { noticeTone=tone; notice.text=message; paintNotice() }
 private fun text(value: String, size: Int = 18) {
  layout.addView(TextView(this).apply {
   text=value; textSize=size.toFloat(); setPadding(0,dp(8),0,dp(8)); setLineSpacing(0f,1.15f)
   // Desde 20 sp son títulos: tinta de bosque y trazo grueso, como los h2 de la consola.
   if(size>=20) { typeface=RupiUi.TITLE; setTextColor(RupiUi.FOREST); if(android.os.Build.VERSION.SDK_INT>=28) isAccessibilityHeading=true } else setTextColor(RupiUi.INK)
  })
 }
 /** Agrupa lo que se dibuje dentro en una tarjeta blanca de la consola y vuelve al contenedor anterior. */
 private fun card(build: () -> Unit): LinearLayout {
  val box = LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL; background=RupiUi.card(this@MainActivity); setPadding(dp(16),dp(12),dp(16),dp(20))
  }
  layout.addView(box, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(10); bottomMargin=dp(6) })
  into(box, build)
  return box
 }
 /** Dibuja dentro de un contenedor ya creado (p. ej., la lista de temas que llega después). */
 private fun into(container: LinearLayout, build: () -> Unit) {
  val outer=layout; layout=container
  try { build() } finally { layout=outer }
 }
 private fun muted(value: String) {
  layout.addView(TextView(this).apply { text=value; textSize=15f; setTextColor(RupiUi.MUTED); setPadding(0,dp(2),0,dp(6)); setLineSpacing(0f,1.15f) })
 }
 /** Encabezado de sección sobre el papel, como los títulos de la consola. */
 private fun section(title: String, subtitle: String? = null) {
  layout.addView(TextView(this).apply {
   text=title; typeface=RupiUi.TITLE; textSize=20f; setTextColor(RupiUi.FOREST); setPadding(dp(2),dp(18),0,dp(2))
   if(android.os.Build.VERSION.SDK_INT>=28) isAccessibilityHeading=true
  })
  if(subtitle!=null) muted(subtitle)
 }
 /** Sello de la consola: etiqueta redonda con borde (estado, tipo de reto, minutos, XP). */
 private fun chip(value: String, back: Int, border: Int, color: Int) {
  layout.addView(TextView(this).apply {
   text=value; typeface=RupiUi.BOLD; textSize=13f; setTextColor(color); background=RupiUi.chip(this@MainActivity,back,border)
   setPadding(dp(12),dp(5),dp(12),dp(5))
  }, LinearLayout.LayoutParams(-2,-2).apply { topMargin=dp(6); bottomMargin=dp(4) })
 }
 /** Fila táctil de una lista: nombre en tinta de bosque, detalle debajo y flecha, con la misma base que los botones. */
 private fun tile(title: String, detail: String?, mark: String = "›", locked: Boolean = false, action: () -> Unit) {
  val row = LinearLayout(this).apply {
   orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.CENTER_VERTICAL; isClickable=true; isFocusable=true
   // Bloqueada se ve plana y color papel, pero sigue respondiendo para explicar por qué no se abre.
   background=if(locked) RupiUi.rounded(Color.rgb(236,232,224), dp(14).toFloat(), dp(2), RupiUi.LINE) else RupiUi.tactile(this@MainActivity,RupiUi.Kind.QUIET)
   minimumHeight=dp(64); setPadding(dp(16),dp(12),dp(14),dp(16))
   contentDescription=if(detail==null) title else "$title. $detail"
   setOnClickListener { if(!busy) action() }
  }
  val texts = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  if(detail!=null) texts.addView(TextView(this).apply { text=detail; textSize=13f; typeface=RupiUi.BOLD; setTextColor(RupiUi.MUTED) })
  texts.addView(TextView(this).apply { text=title; textSize=17f; typeface=RupiUi.BOLD; setTextColor(RupiUi.FOREST) })
  row.addView(texts, LinearLayout.LayoutParams(0,-2,1f))
  row.addView(TextView(this).apply { text=mark; textSize=if(mark=="›") 28f else 22f; setTextColor(if(locked) RupiUi.MUTED else RupiUi.LEAF); setPadding(dp(8),0,0,0); importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO })
  layout.addView(row, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8); bottomMargin=dp(2) })
 }
 /** Recuadro de retroalimentación dentro de una pregunta: verde si acertó, ámbar si toca seguir aprendiendo. */
 private fun feedback(value: String, tone: RupiUi.Tone) {
  layout.addView(TextView(this).apply {
   text=value; textSize=16f; typeface=RupiUi.BOLD; setTextColor(tone.text); setLineSpacing(0f,1.15f)
   background=RupiUi.rounded(tone.back, dp(14).toFloat(), dp(2), tone.border); setPadding(dp(14),dp(12),dp(14),dp(12))
  }, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(10); bottomMargin=dp(4) })
 }
 /** Alternativa de pregunta como fila táctil de 48 dp o más, marcada en cielo al elegirla. */
 private fun styleOption(radio: RadioButton) {
  radio.background=RupiUi.option(this); radio.buttonTintList=android.content.res.ColorStateList.valueOf(RupiUi.SKY)
  radio.textSize=17f; radio.minHeight=dp(52)
  radio.layoutParams=RadioGroup.LayoutParams(-1,-2).apply { topMargin=dp(6); bottomMargin=dp(2) }
 }
 /** HU-39: button that reads instructions or explanations aloud. */
 private fun listen(subject: String, value: () -> String) {
  button("🔊 Escuchar $subject", kind=RupiUi.Kind.SKY) {
   if(voice?.speak(value())!=true) showNotice("Para escuchar, instala o activa una voz en español en los ajustes de Android (Texto a voz).")
  }.contentDescription="Escuchar $subject"
 }
 private fun input(label: String, password: Boolean = false): EditText {
  layout.addView(TextView(this).apply {
   text=label; typeface=RupiUi.BOLD; textSize=15f; setTextColor(RupiUi.FOREST); setPadding(0,dp(12),0,dp(6))
  })
  val field = EditText(this).apply {
   hint=label; contentDescription=label; minHeight=dp(52); isSaveEnabled=false; textSize=17f
   setTextColor(RupiUi.INK); setHintTextColor(Color.rgb(122,135,124)); background=RupiUi.field(this@MainActivity)
   setPadding(dp(14),dp(12),dp(14),dp(12))
   inputType=if(password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
   // Android pone letra de máquina en las contraseñas; se mantiene la misma letra que el resto.
   typeface=android.graphics.Typeface.DEFAULT
  }
  layout.addView(field, LinearLayout.LayoutParams(-1,-2)); return field
 }
 /** Botón táctil de la consola. PRIMARY para la acción principal, QUIET para volver o actualizar, DANGER para salir o reportar. */
 private fun button(label: String, enabled: Boolean = true, kind: RupiUi.Kind = RupiUi.Kind.PRIMARY, action: () -> Unit): Button {
  val button = Button(this).apply {
   text=label; setAllCaps(false); minHeight=dp(52); isEnabled=enabled; typeface=RupiUi.BOLD; textSize=16f
   stateListAnimator=null; elevation=0f
   // Deshabilitado se ve plano y color papel: antes «Comprobar mi respuesta» sin elegir parecía listo para tocar.
   background=RupiUi.tactile(this@MainActivity,kind)
   setTextColor(RupiUi.tactileText(kind))
   setOnClickListener { if(!busy) action() }
  }
  // La base del botón ocupa 4 dp abajo: el texto se centra en la cara.
  button.isSingleLine=false;button.maxLines=Int.MAX_VALUE;button.setHorizontallyScrolling(false);button.minWidth=dp(48);button.setPadding(dp(16),dp(12),dp(16),dp(16))
  layout.addView(button, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(10); bottomMargin=dp(6) })
  return button
 }
 /** Guarda la altura actual para la próxima pantalla: responder no debe mandar al niño arriba. */
 private fun keepPlace() { pendingScroll=root.scrollY }
 private fun restorePlace() {
  val y=restoreScroll
  if(y>0) { restoreScroll=0; root.post { root.scrollTo(0,y) } }
 }
 /** Los mensajes están arriba: si el niño bajó, se sube para que lea lo que pasó. */
 private fun showNotice(message: String, tone: RupiUi.Tone = RupiUi.Tone.INFO) {
  setNotice(message,tone)
  root.post { root.smoothScrollTo(0,0) }
 }
 private fun <T> execute(job: () -> T, done: (T) -> Unit) {
  if(busy) return
  busy=true
  val ticket=screen
  setNotice("Un momento, estamos consultando Rupi…")
  worker.execute {
   try {
    val result=job()
    runOnUiThread { if(alive) { busy=false; if(ticket==screen) { notice.text=""; done(result); if(ticket==screen) restorePlace() } } }
   } catch(e: Exception) {
    runOnUiThread { if(alive) { busy=false; if(e is RupiSessionExpired){StudyAlerts.disable(this);api=null;loginScreen();setNotice(e.message ?: "Vuelve a iniciar sesión.");return@runOnUiThread}; if(ticket==screen) showNotice(e.message ?: "No pudimos continuar. Revisa tu conexión.",RupiUi.Tone.ERROR) } }
   }
  }
 }
 private fun loginScreen() {
  currentTopic=null
  page("Entra a tu aula")
  lateinit var server: EditText; lateinit var user: EditText; lateinit var password: EditText
  card {
   text("¡Hola! ¿Listo para aprender?",22)
   muted("Usa el usuario y la contraseña que te dio tu profe.")
   server=input("Servidor Rupi (HTTPS)")
   if(BuildConfig.DEBUG) server.setText("http://10.0.2.2:8081")
   user=input("Usuario escolar")
   password=input("Contraseña",true)
   button("Entrar") {
    val serverText=server.text.toString();val username=user.text.toString();val secret=password.text.toString()
    password.setText("")
    execute({ val client=RupiApi(serverText);client.login(username,secret);client },{ StudyAlerts.disable(this);api=it; topics() })
   }
  }
  section("Sin conexión")
  button("Resúmenes descargados", kind=RupiUi.Kind.QUIET) {savedSummaries()}
 }
 private fun topics() {
  currentTopic=null;currentRoute=null;currentNode=null
  page("Mis temas")
  card {
   text("Hoy",22)
   muted("Un reto nuevo cada día y tu meta de paradas.")
   button("Reto del día") {dailyChallenge()}
   button("Mi meta de hoy", kind=RupiUi.Kind.LEAF) {dailyGoal()}
  }
  section("Mis rutas","Tu camino de aprendizaje, como en la web. Entra a una ruta para seguir con tus paradas.")
  // La lista llega después: se reserva su lugar para que «Más» quede siempre al final.
  val list = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  layout.addView(list, LinearLayout.LayoutParams(-1,-2))
  section("Más")
  button("Mi ritmo de estudio y avisos", kind=RupiUi.Kind.LEAF){notifications()}
  button("Resúmenes descargados", kind=RupiUi.Kind.QUIET) {savedSummaries()}
  button("Actualizar", kind=RupiUi.Kind.QUIET) { topics() }
  button("Salir", kind=RupiUi.Kind.DANGER) { StudyAlerts.disable(this);execute({ api?.request("/api/v1/auth/logout","POST") },{ api?.clear();api=null;loginScreen() }) }
  execute({ JSONObject(api!!.request("/api/v1/student/learning-routes")).getJSONArray("items") },{ items ->
   into(list) {
    if(items.length()==0) card { muted("Todavía no estás en ninguna ruta. Pídele a tu profe el código de clase para unirte.") }
    for(i in 0 until items.length()) {
     val route=items.getJSONObject(i)
     val done=route.optInt("completedNodes"); val total=route.optInt("totalNodes")
     val grade=route.optJSONObject("grade")?.optString("name").orEmpty()
     val area=route.optJSONObject("area")?.optString("name").orEmpty()
     tile(route.getString("title"), listOf(area,grade,"$done de $total paradas").filter { it.isNotBlank() }.joinToString(" · "),
      if(total>0 && done>=total) "✓" else "›") { routeScreen(route) }
    }
   }
  })
 }

 /** Estado de una parada con las palabras de la web. */
 private fun nodeState(node: JSONObject) = when(node.optString("state")) {
  "COMPLETADO" -> "Completada"
  "EN_CURSO" -> "En curso"
  "DISPONIBLE" -> "Disponible"
  else -> "Bloqueada"
 }

 /** HU-01 y HU-04: las paradas de una ruta agrupadas por unidad MINEDU, con su estado como en «Mi Camino» de la web. */
 private fun routeScreen(route: JSONObject) {
  summaryMode=false;cardMode=false;currentTopic=null;currentRoute=route;currentNode=null
  page(route.getString("title")) { topics() }
  button("Volver a mis temas", kind=RupiUi.Kind.QUIET){topics()}
  execute({ JSONObject(api!!.request("/api/v1/student/learning-routes/"+route.getString("versionRouteId"))) },{ detail ->
   val nodes=detail.getJSONArray("nodes")
   val done=detail.getJSONObject("progress").optInt("completedNodes"); val total=detail.getJSONObject("progress").optInt("totalNodes")
   card {
    val grade=detail.optJSONObject("grade")?.optString("name").orEmpty()
    val area=detail.optJSONObject("area")?.optString("name").orEmpty()
    chip(listOf(area,grade).filter { it.isNotBlank() }.joinToString(" · "),Color.rgb(238,246,251),Color.rgb(147,196,223),RupiUi.SKY_DARK)
    text(detail.getString("title"),22)
    val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{
     max=total.coerceAtLeast(1);progress=done
     progressTintList=android.content.res.ColorStateList.valueOf(RupiUi.LEAF)
     progressBackgroundTintList=android.content.res.ColorStateList.valueOf(RupiUi.PAPER_DEEP)
     importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    layout.addView(bar,LinearLayout.LayoutParams(-1,dp(16)).apply { topMargin=dp(8) })
    muted(if(total>0 && done>=total) "¡Terminaste esta ruta! Puedes repasar cualquier parada." else "$done de $total paradas completadas.")
   }
   val byId=(0 until nodes.length()).associate { nodes.getJSONObject(it).getString("id") to nodes.getJSONObject(it) }
   val shown=mutableSetOf<String>()
   val units=detail.optJSONObject("curriculum")?.optJSONArray("units")
   if(units!=null) for(u in 0 until units.length()) {
    val unit=units.getJSONObject(u)
    val ids=unit.optJSONArray("nodeIds")?.let { a -> (0 until a.length()).map { a.getString(it) } }
     ?: unit.optJSONArray("nodes")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }.orEmpty()
    val inUnit=ids.mapNotNull { byId[it] }.filter { shown.add(it.getString("id")) }
    if(inUnit.isEmpty()) continue
    section(unit.optString("title").ifBlank { "Unidad ${u+1}" })
    inUnit.forEach { nodeTile(route,it) }
   }
   // Paradas fuera de las unidades (o rutas sin unidades): en su orden.
   val rest=(0 until nodes.length()).map { nodes.getJSONObject(it) }.filter { it.getString("id") !in shown }
   if(rest.isNotEmpty()) { if(shown.isNotEmpty()) section("Otras paradas"); rest.forEach { nodeTile(route,it) } }
  })
 }

 private fun nodeTile(route: JSONObject, node: JSONObject) {
  val state=node.optString("state")
  val minutes=node.optInt("estimatedMinutes")
  val detail="Parada ${node.optInt("sequence")} · ${nodeState(node)}"+(if(minutes>0) " · $minutes min" else "")
  when(state) {
   "COMPLETADO" -> tile(node.getString("title"),detail,"✓") { lessonScreen(route,node) }
   "BLOQUEADO" -> tile(node.getString("title"),detail,"🔒",locked=true) {
    showNotice("Esta parada se abre cuando completes la anterior. ¡Tú puedes!")
   }
   else -> tile(node.getString("title"),detail,"›") { lessonScreen(route,node) }
  }
 }

 /**
  * HU-02 y HU-03: abre la lección de la parada como la web. Iniciarla la marca «en curso» y guarda dónde quedó;
  * se lee el contenido, se responden las preguntas y al terminar suma XP y desbloquea la siguiente.
  */
 private fun lessonScreen(route: JSONObject, node: JSONObject, result: JSONObject? = null) {
  summaryMode=false;cardMode=false;currentTopic=null;currentRoute=route;currentNode=node
  if(node.getString("id")!=lessonNodeId){lessonAnswers.clear();lessonNodeId=node.getString("id")}
  val routeId=route.getString("versionRouteId")
  page(node.getString("title")) { routeScreen(route) }
  button("Volver a la ruta", kind=RupiUi.Kind.QUIET){routeScreen(route)}
  execute({ JSONObject(api!!.request("/api/v1/student/learning-routes/$routeId/nodes/"+node.getString("id")+"/start","POST")) },{ lesson ->
   val activityId=lesson.optString("activityId")
   val questions=lesson.optJSONArray("questions") ?: JSONArray()
   val blocks=lesson.optJSONArray("blocks") ?: JSONArray()
   val instructions=lesson.optString("instructions").takeIf { it.isNotBlank() && it!="null" }
   card {
    chip(when(node.optString("activityType")){"RETO"->"Reto";"CUESTIONARIO"->"Cuestionario";else->"Lección"}+" · "+route.getString("title"),
     Color.rgb(238,247,237),Color.rgb(148,208,161),Color.rgb(27,83,41))
    text(lesson.optString("title").ifBlank { node.getString("title") },22)
    if(instructions!=null) muted(instructions)
    listen("la lección") {
     (listOf(lesson.optString("title"))+listOfNotNull(instructions)+(0 until blocks.length()).mapNotNull { blocks.getJSONObject(it).optString("text").takeIf { t -> t.isNotBlank() && t!="null" } }).joinToString(". ")
    }
   }
   for(b in 0 until blocks.length()) {
    val block=blocks.getJSONObject(b)
    var body=block.optString("text").takeIf { it.isNotBlank() && it!="null" }
    var raw=block.optString("url").takeIf { it.isNotBlank() && it!="null" }
    // El panel docente guarda los archivos subidos como «Material adjunto por tu profesor: Nombre (/api/v1/materials/…)»
    // sin url propia: se toma el enlace del texto y se muestra solo el nombre.
    if(block.optString("type")=="ADJUNTO" && body!=null) {
     val embedded=Regex("\\(((?:https?://|/api/)[^()\\s]+)\\)\\s*$").find(body)
     if(embedded!=null) { if(raw==null) raw=embedded.groupValues[1]; body=body.removeRange(embedded.range).trim() }
     body=body.removePrefix("Material adjunto por tu profesor:").trim().ifBlank { null }
    }
    val link=raw?.let { api!!.resource(it) }
    when {
     block.optString("type")=="VIDEO" && link!=null -> card {
      text("Video de la lección",20)
      if(body!=null) muted(body)
      button("▶ Ver el video", kind=RupiUi.Kind.SKY) { open(link) }
     }
     link!=null || block.optString("type")=="ADJUNTO" -> card {
      chip("Material del profesor",Color.rgb(255,248,230),RupiUi.SUN,Color.rgb(107,71,5))
      text(body ?: "Material de clase",18)
      block.optString("accessibleText").takeIf { it.isNotBlank() && it!="null" }?.let { muted(it) }
      if(link!=null) button("Abrir material", kind=RupiUi.Kind.QUIET) { open(link) }
     }
     block.optString("type")=="DESTACADO" && body!=null -> feedback(body,RupiUi.Tone.INFO)
     body!=null -> card { body.split("\n\n").forEach { text(it,17) } }
    }
   }
   if(questions.length()>0) section("Preguntas","Responde para comprobar lo que aprendiste. No cambia tus notas si te equivocas.")
   for(i in 0 until questions.length()) card {
    val q=questions.getJSONObject(i)
    val options=q.getJSONArray("options")
    text("${i+1}. "+q.getString("statement"),20)
    listen("la pregunta ${i+1}") { RupiVoice.question(q.getString("statement"),(0 until options.length()).map{options.getJSONObject(it).getString("text")}) }
    val answer=lessonAnswers[q.getString("id")]
    val group=RadioGroup(this)
    val ids=mutableMapOf<Int,String>()
    for(j in 0 until options.length()) {
     val option=options.getJSONObject(j)
     val radio=RadioButton(this).apply { id=j+1;text=option.getString("text");minWidth=dp(48);setSingleLine(false);setHorizontallyScrolling(false);setPadding(dp(12),dp(12),dp(12),dp(12));isEnabled=answer==null;setTextColor(RupiUi.INK) }
     styleOption(radio)
     ids[j+1]=option.getString("id");group.addView(radio)
     if(answer!=null&&option.getString("id")==answer.optString("correctOptionId")) radio.text="✓ "+option.getString("text")
    }
    layout.addView(group)
    if(answer!=null) {
     val explanation=answer.optString("explanation").ifBlank { q.optString("explanation") }
     feedback((if(answer.getBoolean("isCorrect")) "¡Muy bien! " else "Sigamos aprendiendo. ")+explanation,
      if(answer.getBoolean("isCorrect")) RupiUi.Tone.OK else RupiUi.Tone.RETRY)
     listen("la explicación ${i+1}") { explanation }
    } else {
     val submit=button("Comprobar mi respuesta",false) {
      val selected=ids[group.checkedRadioButtonId] ?: return@button
      execute({ JSONObject(api!!.request("/api/v1/student/activities/$activityId/questions/answer","POST",JSONObject().put("questionId",q.getString("id")).put("selectedOptionId",selected))) },
       { lessonAnswers[q.getString("id")]=it;keepPlace();lessonScreen(route,node) })
     }
     group.setOnCheckedChangeListener { _,checked -> submit.isEnabled=ids.containsKey(checked) }
    }
   }
   if(result!=null) {
    val xp=result.optInt("experienceEarned")
    val message=result.optString("rupiMessage").ifBlank { "¡Parada completada!" }
    feedback(message+(if(xp>0 && !message.contains("XP")) " +$xp XP" else ""),RupiUi.Tone.OK)
    result.optJSONObject("badgeEarned")?.let { badge -> chip("🏅 Insignia: "+badge.optString("name"),Color.rgb(255,248,230),RupiUi.SUN,Color.rgb(107,71,5)) }
    if(result.optBoolean("isRouteCompleted")) feedback("¡Terminaste toda la ruta! "+(result.optInt("routeBonusExperience").takeIf { it>0 }?.let { "+$it XP de bonificación." } ?: ""),RupiUi.Tone.OK)
    val next=result.optJSONObject("nextUnlockedNode")
    if(next!=null) button("Ir a la siguiente parada: "+next.optString("title")) {
     lessonScreen(route,JSONObject().put("id",next.getString("id")).put("title",next.optString("title")).put("sequence",next.optInt("sequence")).put("activityType","LECCION").put("state","DISPONIBLE"))
    }
   } else {
    val answered=(0 until questions.length()).all { lessonAnswers.containsKey(questions.getJSONObject(it).getString("id")) }
    val completed=lesson.optString("state")=="COMPLETADO" || node.optString("state")=="COMPLETADO"
    if(completed && !answered) feedback("Ya completaste esta parada. Puedes repasarla cuando quieras.",RupiUi.Tone.OK)
    else button(if(answered) "Terminar parada" else "Responde las ${questions.length()} preguntas para terminar",answered) {
     execute({ JSONObject(api!!.request("/api/v1/student/learning-routes/$routeId/nodes/"+node.getString("id")+"/complete","POST")) },
      { keepPlace();lessonScreen(route,node.put("state","COMPLETADO"),it) })
    }
   }
   section("Más para repasar","Recursos extra de esta parada: aparecen cuando tu profe los prepara o la IA está activa.")
   val topic=JSONObject().put("routeId",routeId).put("nodeId",node.getString("id")).put("title",node.getString("title"))
    .put("routeTitle",route.getString("title")).put("route",route)
   button("Practicar con Rupi (IA)", kind=RupiUi.Kind.LEAF) { loadTopic(topic) }
   button("Repasar con tarjetas", kind=RupiUi.Kind.QUIET) { loadCards(topic) }
   button("Ver resumen visual", kind=RupiUi.Kind.QUIET) { loadSummary(topic) }
  })
 }

 /** Abre un video o material fuera de la app (YouTube, visor de PDF o navegador). */
 private fun open(link: String) {
  try { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))) }
  catch (_: android.content.ActivityNotFoundException) { showNotice("No hay una app para abrir este enlace en el teléfono.",RupiUi.Tone.ERROR) }
 }

 private fun dailyGoal() {
  summaryMode=false;cardMode=false;currentTopic=null;currentRoute=null;currentNode=null
  page("Mi meta de hoy") { topics() }
  button("Volver a mis temas", kind=RupiUi.Kind.QUIET){topics()}
  button("Actualizar mi avance", kind=RupiUi.Kind.QUIET){dailyGoal()}
  execute({ Pair(JSONObject(api!!.request("/api/v1/student/daily-goal")),JSONObject(api!!.request("/api/v1/student/daily-goal/comparison"))) },{ (overview,comparison) ->
   val today=overview.getJSONObject("today")
   val completed=today.getInt("completedLessons");val target=today.getInt("targetLessons");val met=today.getBoolean("isGoalMet")
   val lessons=if(target==1) "lección" else "lecciones"
   card {
    layout.addView(DailyGoalRing(this,completed,target,met),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8);bottomMargin=dp(8)})
    val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{
     max=target.coerceAtLeast(1);progress=completed.coerceAtMost(max);minimumHeight=dp(16)
     progressTintList=android.content.res.ColorStateList.valueOf(if(met) RupiUi.SUN else RupiUi.LEAF)
     progressBackgroundTintList=android.content.res.ColorStateList.valueOf(RupiUi.PAPER_DEEP)
     importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    layout.addView(bar,LinearLayout.LayoutParams(-1,dp(16)))
    text(if(met) "¡Meta del día lograda! Completaste $completed de $target $lessons." else "Llevas $completed de $target $lessons. Te faltan "+today.getInt("pendingLessons")+".",20)
    muted(today.getString("motivationalMessage"))
    val streak=overview.optInt("currentStreakDays")
    if(streak>0) chip("🔥 Racha: $streak "+(if(streak==1) "día seguido" else "días seguidos"),Color.rgb(255,248,230),RupiUi.SUN,Color.rgb(107,71,5))
    listen("mi avance de hoy") { "Llevas $completed de $target $lessons. "+today.getString("motivationalMessage")+" "+comparison.getString("message") }
   }
   card { scoreComparison(comparison) }
   card {
    text("Tu semana de estudio",20)
    val week=overview.getJSONArray("weeklyHistory")
    for(i in 0 until week.length()){
     val day=week.getJSONObject(i)
     val name=if(day.getBoolean("isToday")) "Hoy" else day.getString("dayOfWeek")
     if(day.getBoolean("isGoalMet")) chip("✓ $name · meta cumplida",Color.rgb(238,247,237),Color.rgb(148,208,161),Color.rgb(27,83,41))
     else chip("$name · "+day.getInt("completedLessons")+" de "+day.getInt("targetLessons"),Color.WHITE,RupiUi.LINE,RupiUi.MUTED)
    }
   }
   section("¿Cuántas paradas quieres completar por día?","Es la misma meta que ves en la web y en tus avisos. No cambia tus notas.")
   val options=overview.getJSONArray("availableTargetOptions")
   for(i in 0 until options.length()){
    val option=options.getInt(i)
    val current=option==overview.getInt("currentDailyTargetSetting")
    // La meta elegida se marca en verde hoja; antes se veía deshabilitada, como si no se pudiera usar.
    button((if(current) "✓ " else "")+"$option "+(if(option==1) "parada" else "paradas"),true,if(current) RupiUi.Kind.LEAF else RupiUi.Kind.QUIET){
     if(!current) execute({ api!!.request("/api/v1/student/daily-goal/target?target=$option","PUT") },{ dailyGoal() })
    }
   }
  })
 }

 /** HU-40: daily logical reasoning or reading challenge with Peruvian context. Answers stay in memory until it is finished. */
 private fun dailyChallenge(result: JSONObject? = null) {
  summaryMode=false;cardMode=false;currentTopic=null;currentRoute=null;currentNode=null
  page("Reto del día") { topics() }
  button("Volver a mis temas", kind=RupiUi.Kind.QUIET){topics()}
  execute({ JSONObject(api!!.request("/api/v1/student/daily-challenge")) },{ challenge ->
   // The bank can repeat a challenge on another day: answers only count for the day they were given.
   if(challenge.getString("date")!=challengeDay){challengeAnswers.clear();challengeDay=challenge.getString("date")}
   // Lo respondido hoy viene del servidor: al reabrir la app no hay que responder otra vez.
   val saved=challenge.optJSONArray("answeredToday")
   if(saved!=null)for(i in 0 until saved.length()){val answer=saved.getJSONObject(i);challengeAnswers.putIfAbsent(answer.getString("questionId"),answer)}
   val activity=challenge.getString("activityVersionId")
   val reading=if(challenge.isNull("readingText")) "" else challenge.getString("readingText")
   val description=challenge.optString("description")
   val reading_=reading
   card {
    // Lectura en cielo y lógica en naranja, como las bandas del reto en la web.
    if(challenge.getString("skill")=="COMPRENSION_LECTORA") chip("Comprensión lectora · "+challenge.getInt("estimatedMinutes")+" min · +"+challenge.getInt("experienceReward")+" XP",
     Color.rgb(238,246,251),Color.rgb(147,196,223),RupiUi.SKY_DARK)
    else chip("Razonamiento lógico · "+challenge.getInt("estimatedMinutes")+" min · +"+challenge.getInt("experienceReward")+" XP",
     Color.rgb(255,244,240),Color.rgb(240,160,140),Color.rgb(127,36,16))
    text(challenge.getString("title"),22)
    if(description.isNotBlank()) muted(description)
    listen("el reto") { challenge.getString("title")+". "+description+(if(reading_.isNotBlank()) " Lectura. $reading_" else "") }
   }
   if(result!=null || challenge.getBoolean("isCompleted")) {
    feedback(result?.getString("rupiMessage") ?: "¡Ya completaste el reto de hoy! Mañana Rupi te trae uno nuevo.",RupiUi.Tone.OK)
    return@execute
   }
   if(reading.isNotBlank()) card {
    text("Lectura",20)
    reading.split("\n\n").forEach { text(it) }
    listen("la lectura") { reading }
   }
   val questions=challenge.getJSONArray("questions")
   for(i in 0 until questions.length()) card {
    val q=questions.getJSONObject(i)
    val options=q.getJSONArray("options")
    text("${i+1}. "+q.getString("statement"),20)
    listen("la pregunta ${i+1}") { RupiVoice.question(q.getString("statement"),(0 until options.length()).map{options.getJSONObject(it).getString("text")}) }
    val feedback=challengeAnswers[q.getString("id")]
    val group=RadioGroup(this)
    val ids=mutableMapOf<Int,String>()
    for(j in 0 until options.length()) {
     val option=options.getJSONObject(j)
     val radio=RadioButton(this).apply { id=j+1;text=option.getString("text");minHeight=dp(48);minWidth=dp(48);setSingleLine(false);setHorizontallyScrolling(false);setPadding(dp(12),dp(12),dp(12),dp(12));isEnabled=feedback==null;setTextColor(RupiUi.INK) }
     styleOption(radio)
     ids[j+1]=option.getString("id");group.addView(radio)
     if(feedback!=null&&option.getString("id")==feedback.optString("correctOptionId")) radio.text="✓ "+option.getString("text")
    }
    layout.addView(group)
    if(feedback!=null) {
     val explanation=feedback.optString("explanation").ifBlank { q.optString("explanation") }
     feedback((if(feedback.getBoolean("isCorrect")) "¡Muy bien! " else "Sigamos aprendiendo. ")+explanation,
      if(feedback.getBoolean("isCorrect")) RupiUi.Tone.OK else RupiUi.Tone.RETRY)
     listen("la explicación ${i+1}") { explanation }
    } else {
     val submit=button("Comprobar mi respuesta",false) {
      val selected=ids[group.checkedRadioButtonId] ?: return@button
      execute({ JSONObject(api!!.request("/api/v1/student/activities/$activity/questions/answer","POST",JSONObject().put("questionId",q.getString("id")).put("selectedOptionId",selected))) },
       { challengeAnswers[q.getString("id")]=it;keepPlace();dailyChallenge() })
     }
     group.setOnCheckedChangeListener { _,checked -> submit.isEnabled=ids.containsKey(checked) }
    }
   }
   val ready=(0 until questions.length()).all { challengeAnswers.containsKey(questions.getJSONObject(it).getString("id")) }
   button(if(ready) "Terminar el reto" else "Responde las ${questions.length()} preguntas para terminar",ready) {
    execute({ JSONObject(api!!.request("/api/v1/student/daily-challenge/"+challenge.getString("id")+"/complete","POST")) },{ dailyChallenge(it) })
   }
  })
 }

 /** Short comparison of today's XP against yesterday's (HU-36). */
 private fun scoreComparison(comparison: JSONObject) {
  text("Hoy vs. ayer",20)
  val todayXp=comparison.getInt("todayExperience");val yesterdayXp=comparison.getInt("yesterdayExperience")
  val max=maxOf(todayXp,yesterdayXp,1)
  fun row(label: String,xp: Int,lessons: Int,color: Int) {
   text("$label · $xp XP · $lessons "+(if(lessons==1) "parada" else "paradas"),16)
   val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{
    this.max=max;progress=xp;progressTintList=android.content.res.ColorStateList.valueOf(color)
    progressBackgroundTintList=android.content.res.ColorStateList.valueOf(RupiUi.PAPER_DEEP)
    importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
   }
   layout.addView(bar,LinearLayout.LayoutParams(-1,dp(16)))
  }
  row("Hoy",todayXp,comparison.getInt("todayLessons"),RupiUi.LEAF)
  row("Ayer",yesterdayXp,comparison.getInt("yesterdayLessons"),Color.rgb(167,154,133))
  feedback(when(comparison.getString("trend")){"SUBE"->"↑ ";"BAJA"->"↓ ";else->"= "}+comparison.getString("message"),
   if(comparison.getString("trend")=="BAJA") RupiUi.Tone.RETRY else RupiUi.Tone.OK)
 }

 private fun notifications() {
  summaryMode=false;cardMode=false;currentTopic=null;currentRoute=null;currentNode=null
  page("Mi ritmo de estudio") { topics() }
  button("Volver a mis temas", kind=RupiUi.Kind.QUIET){topics()}
  execute({
   Pair(JSONObject(api!!.request("/api/v1/student/notifications/preferences")),JSONObject(api!!.request("/api/v1/student/notifications")))
  },{ (status,inbox) ->
   val prefs=status.getJSONObject("preferences")
   card {
    text("Mi meta y mis avisos",22)
    muted("Completaste "+status.getInt("completedToday")+" actividades hoy. Las metas no cambian tus notas; las horas son de Perú.")
    layout.addView(TextView(this).apply { text="Mi meta diaria"; typeface=RupiUi.BOLD; textSize=15f; setTextColor(RupiUi.FOREST); setPadding(0,dp(12),0,dp(6)) })
    val goal=Spinner(this).apply{
     minimumHeight=dp(52);contentDescription="Mi meta diaria";background=RupiUi.field(this@MainActivity);setPadding(dp(10),dp(6),dp(10),dp(6))
     adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,(1..5).map{"$it actividad(es)"})
     setSelection(prefs.getInt("dailyGoal")-1)
    };layout.addView(goal)
    layout.addView(TextView(this).apply { text="¿Qué avisos quieres?"; typeface=RupiUi.BOLD; textSize=15f; setTextColor(RupiUi.FOREST); setPadding(0,dp(16),0,dp(2)) })
    fun toggle(label:String,value:Boolean)=CheckBox(this).apply{
     text=label;isChecked=value;minHeight=dp(52);setSingleLine(false);setHorizontallyScrolling(false);setPadding(dp(8),dp(12),dp(8),dp(12))
     textSize=16f;setTextColor(RupiUi.INK);buttonTintList=android.content.res.ColorStateList.valueOf(RupiUi.LEAF)
     this@MainActivity.layout.addView(this)
    }
    val goals=toggle("Recordar mi meta pendiente y mi racha",prefs.getBoolean("goals"))
    val evaluations=toggle("Avisos de mi docente: fechas de evaluación y materiales nuevos",prefs.getBoolean("evaluations"))
    val system=toggle("Permitir avisos en Android",prefs.getBoolean("system"))
    val at=input("Hora del recordatorio (HH:mm)").apply{setText(prefs.getString("remindAt"))}
    val from=input("Descansar desde (HH:mm)").apply{setText(prefs.getString("quietFrom"))}
    val until=input("Volver a recibir avisos (HH:mm)").apply{setText(prefs.getString("quietUntil"))}
    button("Guardar mis preferencias"){
     // El servidor solo activa los avisos del teléfono para las categorías marcadas: sin ninguna, se apagaban sin decir por qué.
     if(system.isChecked&&!goals.isChecked&&!evaluations.isChecked){
      showNotice("Para recibir avisos en el teléfono, marca también qué avisos quieres: tu meta y racha, o los de tu docente.")
      return@button
     }
     val value=JSONObject().put("dailyGoal",goal.selectedItemPosition+1).put("goals",goals.isChecked)
      .put("evaluations",evaluations.isChecked).put("system",system.isChecked)
      .put("remindAt",at.text.toString()).put("quietFrom",from.text.toString()).put("quietUntil",until.text.toString()).put("revision",prefs.getLong("revision"))
     execute({JSONObject(api!!.request("/api/v1/student/notifications/preferences","PUT",value))},{
      prefs.put("revision",it.getJSONObject("preferences").getLong("revision"))
      if(it.getJSONObject("preferences").getBoolean("system")){
       if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),25)
       else activateAlerts()
      }else{StudyAlerts.disable(this);showNotice("Preferencias guardadas. Avisos del dispositivo desactivados.",RupiUi.Tone.OK)}
     })
    }
   }
   button("Recargar preferencias guardadas", kind=RupiUi.Kind.QUIET){notifications()}
   muted("Android consulta periódicamente con conexión. El ahorro de batería puede retrasar los avisos. Si cierras la sesión, se detienen.")
   section("Mis avisos")
   if(inbox.optBoolean("quiet"))feedback("Horario de descanso: los avisos del dispositivo están pausados.",RupiUi.Tone.INFO)
   val items=inbox.getJSONArray("items")
   if(items.length()==0)card { muted("No tienes recordatorios pendientes.") }
   for(i in 0 until items.length()) card {
    val item=items.getJSONObject(i);text(item.getString("title"),20);text(item.getString("body"),16)
    if(item.getBoolean("read"))chip("✓ Leído",Color.rgb(238,247,237),Color.rgb(148,208,161),Color.rgb(27,83,41)) else button("Marcar como leído", kind=RupiUi.Kind.QUIET){
     execute({api!!.request("/api/v1/student/notifications/"+item.getString("id")+"/read","PUT")},{keepPlace();notifications()})
    }
   }
  })
 }
 private fun activateAlerts(){
  if(!StudyAlerts.allowed(this)){showNotice("Preferencias guardadas. Permite las notificaciones en los ajustes de Android para recibir avisos.");return}
  execute({StudyAlerts.enable(this,api!!)},{showNotice("Preferencias guardadas. Avisos activados en este dispositivo.",RupiUi.Tone.OK)})
 }
 override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,results:IntArray){
  super.onRequestPermissionsResult(code,permissions,results)
  if(code==25 && api!=null){
   if(results.isNotEmpty()&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)activateAlerts()
   else showNotice("Puedes consultar tus avisos aquí aunque no concedas el permiso de Android.")
  }
 }

 private fun path(topic: JSONObject) = "/api/v1/student/learning-routes/"+topic.getString("routeId")+"/nodes/"+topic.getString("nodeId")+"/ai-quiz"
 /** La práctica, las tarjetas y el resumen se abren desde la lección: «Atrás» vuelve a esa lección. */
 private fun backFromTopic(topic: JSONObject) {
  val route=topic.optJSONObject("route")
  if(route!=null) lessonScreen(route,JSONObject().put("id",topic.getString("nodeId")).put("title",topic.getString("title"))) else topics()
 }
 private fun loadTopic(topic: JSONObject) {
  summaryMode=false;cardMode=false
  currentTopic=topic
  page(topic.getString("title")) { backFromTopic(topic) }
  button("Volver a la lección", kind=RupiUi.Kind.QUIET) { backFromTopic(topic) }
  button("Consultar mi práctica", kind=RupiUi.Kind.QUIET) { loadTopic(topic) }
  button("Ver resumen visual", kind=RupiUi.Kind.LEAF) {loadSummary(topic)}
  button("Repasar con tarjetas", kind=RupiUi.Kind.LEAF) { loadCards(topic) }
  execute({ JSONObject(api!!.request(path(topic))) },{ status ->
   val practice=status.optJSONObject("practice")
   text("Práctica formativa: no cambia tus calificaciones.")
   if(!status.optBoolean("configured")) text("La generación con IA todavía no está habilitada. Podrás usar cuestionarios revisados cuando estén disponibles.")
   if(practice!=null) renderPractice(practice,topic)
   if(practice==null || practice.optString("status") !in setOf("LISTO","GENERANDO")) {
    button("Practicar con Rupi",status.optBoolean("configured")||status.optBoolean("reviewedAvailable")) {
     val key=topic.getString("nodeId")
     execute({ JSONObject(api!!.request(path(topic),"POST",JSONObject().put("requestId",api!!.requestId(key)))) },{
      if(it.optString("status")!="GENERANDO") api!!.pending.remove(key)
      loadTopic(topic)
     })
    }
   }
  })
 }
 private fun renderPractice(view: JSONObject,topic: JSONObject) {
  val state=view.getString("status")
  if(state=="GENERANDO") { text("Preparando preguntas. Consulta tu práctica en unos segundos.");return }
  if(state=="FALLIDO") { text("No pudimos preparar el cuestionario. Puedes seguir estudiando tu lección.");return }
  if(state=="RETIRADO") { text("Cuestionario retirado para revisión docente.");return }
  text(if(view.optString("source")=="REUTILIZADO") "Cuestionario existente revisado por docente." else "Cuestionario asistido por IA · Puede contener errores.")
  text(if(view.optString("reviewState")=="REVISADO_DOCENTE") "Revisión docente realizada." else "Validación automática; no sustituye la revisión docente.")
  val questions=view.getJSONArray("questions")
  for(i in 0 until questions.length()) card {
   val q=questions.getJSONObject(i)
   text(q.getString("text"),20)
   val group=RadioGroup(this)
   val options=q.getJSONArray("options")
   listen("la pregunta") { RupiVoice.question(q.getString("text"),(0 until options.length()).map{options.getJSONObject(it).getString("text")}) }
   val ids=mutableMapOf<Int,String>()
   val answered=!q.isNull("selectedOption")
   for(j in 0 until options.length()) {
    val option=options.getJSONObject(j)
    val radio=RadioButton(this).apply { id=j+1;text=option.getString("text");minHeight=dp(48);minWidth=dp(48);setSingleLine(false);setHorizontallyScrolling(false);setPadding(dp(12),dp(12),dp(12),dp(12));isEnabled=!answered;setTextColor(RupiUi.INK) }
    styleOption(radio)
    ids[j+1]=option.getString("id");group.addView(radio)
    if(answered&&option.getString("id")==q.optString("selectedOption")) group.check(j+1)
   }
   layout.addView(group)
   if(answered) {
    feedback((if(q.optBoolean("correct")) "¡Buen trabajo! " else "Sigamos aprendiendo. ")+q.optString("explanation"),
     if(q.optBoolean("correct")) RupiUi.Tone.OK else RupiUi.Tone.RETRY)
    listen("la explicación") { q.optString("explanation") }
    if(!q.optBoolean("correct")) button("¿Por qué me equivoqué?", kind=RupiUi.Kind.SKY) {
     execute({ JSONObject(api!!.request("/api/v1/student/ai-quizzes/"+view.getString("practiceId")+"/questions/"+q.getString("id")+"/hint","POST")) },{ showHint(it) })
    }
   }
   else {
    val submit=button("Comprobar mi respuesta",false) {
     val selected=ids[group.checkedRadioButtonId] ?: return@button
     execute({ api!!.request("/api/v1/student/ai-quizzes/"+view.getString("practiceId")+"/answers","POST",
      JSONObject().put("questionId",q.getString("id")).put("optionId",selected)) },{ keepPlace();loadTopic(topic) })
    }
    group.setOnCheckedChangeListener { _,checked -> submit.isEnabled=ids.containsKey(checked) }
   }
   button("Reportar esta pregunta", kind=RupiUi.Kind.DANGER) {
    val reason=EditText(this).apply { hint="¿Qué debemos revisar?";minHeight=dp(48);filters=arrayOf(android.text.InputFilter.LengthFilter(500)) }
    AlertDialog.Builder(this).setTitle("Revisar pregunta").setView(reason).setNegativeButton("Cancelar",null)
     .setPositiveButton("Enviar") { _,_ ->
      if(reason.text.isNotBlank()) execute({ api!!.request("/api/v1/student/ai-quizzes/"+view.getString("practiceId")+"/reports","POST",
       JSONObject().put("questionId",q.getString("id")).put("reason",reason.text.toString())) },{ loadTopic(topic) })
     }.show()
   }
  }
  if(state=="COMPLETADO") feedback("¡Terminaste! "+view.getInt("correct")+" de "+questions.length()+" aciertos. Revisa las explicaciones.",RupiUi.Tone.OK)
 }

 /** HU-37: explanation of a wrong answer adapted to the student's grade, with a real-life example (HU-38). */
 private fun showHint(hint: JSONObject) {
  val level=when(hint.getString("level")){"INICIAL"->"1.° y 2.° grado";"INTERMEDIO"->"3.° y 4.° grado";else->"5.° y 6.° grado"}
  val steps=hint.getJSONArray("steps")
  val body=StringBuilder(hint.getString("hint")).append("\n")
  for(i in 0 until steps.length()) body.append("\n").append(i+1).append(". ").append(steps.getString(i))
  if(hint.optString("realLifeExample").isNotBlank()) body.append("\n\nEjemplo de la vida real:\n").append(hint.getString("realLifeExample"))
  body.append("\n\n").append(if(hint.optString("source")=="IA") "Pista asistida por IA · revisada automáticamente." else "Basada en la explicación revisada de la pregunta.")
  AlertDialog.Builder(this).setTitle("Rupi te explica · $level"+(if(hint.optBoolean("reinforced")) " · paso a paso" else ""))
   .setMessage(body.toString()).setPositiveButton("Entendido",null)
   .setNeutralButton("Escuchar") { _,_ -> if(voice?.speak(body.toString())!=true) showNotice("Para escuchar, instala o activa una voz en español en los ajustes de Android (Texto a voz).") }.show()
 }

 private fun cardsPath(topic: JSONObject) = path(topic).removeSuffix("ai-quiz")+"flashcards"
 private fun loadCards(topic: JSONObject) {
  currentTopic=topic
  cardMode=true
  page("Repasar con tarjetas") { cardMode=false;loadTopic(topic) }
  button("Volver al tema", kind=RupiUi.Kind.QUIET) { cardMode=false;loadTopic(topic) }
  button("Recuperar mi repaso", kind=RupiUi.Kind.QUIET) { loadCards(topic) }
  execute({ JSONObject(api!!.request(cardsPath(topic))) },{ renderCards(topic,it) })
 }
 private fun renderCards(topic: JSONObject,deck: JSONObject) {
  text("Piensa en la respuesta y voltea la tarjeta. Tu autoevaluación no cambia tus notas.")
  val cards=deck.getJSONArray("cards")
  if(cards.length()==0) { text("Todavía no hay tarjetas revisadas para esta actividad.");return }
  var index=0
  for(i in 0 until cards.length()) if(cards.getJSONObject(i).getString("id")==deck.optString("current")) index=i
  val card=cards.getJSONObject(index)
  val revealed=deck.optBoolean("revealed")
  fun command(action: String,id: String=card.getString("id")) {
   val body=JSONObject().put("requestId",java.util.UUID.randomUUID().toString()).put("revision",deck.getLong("revision")).put("action",action).put("cardId",id)
   execute({ api!!.request(cardsPath(topic),"POST",body) },{ loadCards(topic) })
  }
  fun move(delta: Int) { val next=index+delta;if(next in 0 until cards.length()) command("NAVIGATE",cards.getJSONObject(next).getString("id")) }
  text("Tarjeta ${index+1} de ${cards.length()} · "+when(card.getString("state")){"PRACTICAR"->"Necesito practicar";"RECUERDO"->"Lo recuerdo";else->"Por descubrir"})
  val face=button((if(revealed) "Respuesta: " else "Pregunta: ")+card.getString(if(revealed) "answer" else "question")) { command(if(revealed) "NAVIGATE" else "REVEAL") }
  face.minHeight=dp(200)
  face.setBackgroundColor(if(revealed) Color.rgb(233,243,231) else Color.rgb(255,245,216))
  face.setTextColor(Color.rgb(23,62,48))
  var startX=0f;var startY=0f;var dragged=false
  face.setOnTouchListener { _,event ->
   when(event.actionMasked) {
    android.view.MotionEvent.ACTION_DOWN -> {startX=event.x;startY=event.y;dragged=false;false}
    android.view.MotionEvent.ACTION_MOVE -> {
     val dx=event.x-startX;val dy=event.y-startY
     if(kotlin.math.abs(dx)>dp(10)||kotlin.math.abs(dy)>dp(10))dragged=true
     if(kotlin.math.abs(dx)>kotlin.math.abs(dy)*1.5 && kotlin.math.abs(dx)>dp(10))face.parent.requestDisallowInterceptTouchEvent(true)
     dragged
    }
    android.view.MotionEvent.ACTION_UP -> {
     val dx=event.x-startX;val dy=event.y-startY
     face.parent.requestDisallowInterceptTouchEvent(false);face.isPressed=false
     if(!busy&&kotlin.math.abs(dx)>dp(60)&&kotlin.math.abs(dx)>kotlin.math.abs(dy)*1.5) {move(if(dx<0)1 else -1);true} else { if(!dragged)face.performClick();true }
    }
    android.view.MotionEvent.ACTION_CANCEL -> {dragged=true;face.isPressed=false;face.parent.requestDisallowInterceptTouchEvent(false);true}
    else -> false
   }
  }
  listen(if(revealed) "la respuesta" else "la pregunta") { card.getString(if(revealed) "answer" else "question") }
  button(if(revealed) "Ver pregunta" else "Ver respuesta", kind=RupiUi.Kind.QUIET) {command(if(revealed)"NAVIGATE" else "REVEAL")}
  button("Anterior",index>0, kind=RupiUi.Kind.QUIET) {move(-1)}
  button("Siguiente",index<cards.length()-1, kind=RupiUi.Kind.QUIET) {move(1)}
  if(revealed) {
   button("Lo recuerdo") {command("REMEMBER")}
   button("Necesito practicar", kind=RupiUi.Kind.QUIET) {command("PRACTICE")}
  }
  text("Fuente indicada por tu docente: "+card.getString("source"))
  text("Primero repasamos lo que necesita práctica; después las tarjetas nuevas y lo que recuerdas.")
  button("Volver a repasar", kind=RupiUi.Kind.QUIET) {command("RESTART")}
 }

 private fun summaryPath(topic: JSONObject)=path(topic).removeSuffix("ai-quiz")+"visual-summary"
 private fun summaryFile(topic: JSONObject): java.io.File {
  val folder=java.io.File(filesDir,"visual-summaries");folder.mkdirs()
  return java.io.File(folder,java.util.UUID.fromString(topic.getString("routeId")).toString()+"-"+java.util.UUID.fromString(topic.getString("nodeId")).toString()+".json")
 }
 private fun savedSummaries() {
  summaryMode=false;currentTopic=null
  page("Resúmenes descargados") { if(api==null)loginScreen() else topics() }
  text("Copias guardadas en este dispositivo. Pueden estar desactualizadas; vuelve al tema con conexión para comprobar su vigencia.")
  button("Volver", kind=RupiUi.Kind.QUIET) {if(api==null)loginScreen() else topics()}
  val files=java.io.File(filesDir,"visual-summaries").listFiles().orEmpty()
  if(files.isEmpty())text("Todavía no guardaste resúmenes.")
  files.forEach { file ->
   try {
    val saved=JSONObject(file.readText(Charsets.UTF_8));val topic=saved.getJSONObject("topic")
    button(topic.getString("title"), kind=RupiUi.Kind.LEAF) {
     currentTopic=topic;summaryMode=true;page("Copia descargada") { summaryMode=false;savedSummaries() }
     button("Volver a mis descargas", kind=RupiUi.Kind.QUIET) {savedSummaries()}
     if(api!=null)button("Comprobar vigencia", kind=RupiUi.Kind.QUIET) {loadSummary(topic)}
     renderSummary(topic,saved.getJSONObject("resource"),true)
    }
   }catch(_:Exception){text("No pudimos leer una descarga.")}
  }
 }
 private fun loadSummary(topic: JSONObject) {
  summaryMode=true;cardMode=false;currentTopic=topic
  page("Ver resumen visual") { summaryMode=false;loadTopic(topic) }
  button("Volver al tema", kind=RupiUi.Kind.QUIET) {summaryMode=false;loadTopic(topic)}
  button("Comprobar versión y revisión", kind=RupiUi.Kind.QUIET) {loadSummary(topic)}
  execute({
   try {JSONObject(api!!.request(summaryPath(topic))).put("offline",false)}
   catch(e:Exception){
    val file=summaryFile(topic)
    if(!file.exists())throw e
    JSONObject().put("resource",JSONObject(file.readText(Charsets.UTF_8)).getJSONObject("resource")).put("offline",true)
   }
  },{ result ->
   val resource=result.optJSONObject("resource");val offline=result.optBoolean("offline")
   if(resource!=null) {
    if(!offline) {
     val file=summaryFile(topic)
     val outdated=if(file.exists()) try {JSONObject(file.readText(Charsets.UTF_8)).getJSONObject("resource").optString("version")!=resource.optString("version")} catch(_:Exception){true} else false
     if(outdated||resource.optString("state") in setOf("REPORTADO","RECHAZADO","DESACTUALIZADO")){file.delete();text("La copia anterior cambió o fue retirada. Comprueba este recurso antes de guardarlo nuevamente.")}
    }
    renderSummary(topic,resource,offline)
   }else{
    if(!offline)summaryFile(topic).delete()
    text("No hay un resumen disponible para esta versión del tema.")
   }
   if(!offline && result.optBoolean("configured") && (resource==null||resource.optString("state")=="FALLIDO"))
    button("Preparar resumen visual") {
     execute({api!!.request(summaryPath(topic),"POST",JSONObject().put("requestId",java.util.UUID.randomUUID().toString()))},{loadSummary(topic)})
    }
   if(!result.optBoolean("configured") && resource==null)text("La IA todavía no está configurada. Puedes continuar tu lección.")
  })
 }
 private fun renderSummary(topic: JSONObject,resource: JSONObject,offline: Boolean) {
  if(offline)text("Copia descargada. No pudimos comprobar su vigencia. Puede haber una versión nueva o estar retirada.")
  val state=resource.optString("state")
  val content=resource.optJSONObject("content")
  if(content==null){text(when(state){"GENERANDO"->"Preparando el resumen. Consulta nuevamente en unos segundos.";"FALLIDO"->"No pudimos preparar el resumen. Continúa tu lección.";else->"Resumen retirado o pendiente de revisión."});return}
  text("Contenido asistido por IA · "+resource.optInt("grade")+".º de primaria")
  text(if(state=="REVISADO_DOCENTE")"Revisado por docente." else "Validación automática; puede contener errores.")
  text(content.getString("title"),24)
  listen("el resumen") {
   val parts=mutableListOf(content.getString("title"))
   val c=content.getJSONArray("concepts");for(i in 0 until c.length())parts.add(c.getJSONObject(i).getString("title")+": "+c.getJSONObject(i).getString("text"))
   val s=content.getJSONArray("steps");for(i in 0 until s.length())parts.add("Paso ${i+1}: "+s.getString(i))
   parts.add("Ejemplo explicado: "+content.getString("example"));parts.add("Para recordar: "+content.getString("remember"))
   parts.joinToString(" ")
  }
  val concepts=content.getJSONArray("concepts")
  for(i in 0 until concepts.length()){
   val c=concepts.getJSONObject(i);text("${i+1}. "+c.getString("title"),20);text(c.getString("text"))
  }
  text("Paso a paso",20);val steps=content.getJSONArray("steps")
  for(i in 0 until steps.length())text("${i+1}. "+steps.getString(i))
  text("Ejemplo explicado",20);text(content.getString("example"))
  text("Para recordar",20);text(content.getString("remember"))
  text("Fuente: "+resource.optString("source"))
  if(!offline) {
   button("Guardar para consultar sin conexión", kind=RupiUi.Kind.LEAF){
    execute({
     val bytes=api!!.download(summaryPath(topic)+"/"+resource.getString("id")+"/package")
     var payload:JSONObject?=null
     java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zip ->
      var entry=zip.nextEntry
      while(entry!=null){if(entry.name=="contenido.json")payload=JSONObject(zip.readBytes().toString(Charsets.UTF_8));entry=zip.nextEntry}
     }
     val saved=payload?:throw IllegalStateException("Paquete incompleto.")
     val file=summaryFile(topic);val temporary=java.io.File(file.parentFile,file.name+".tmp")
     temporary.writeText(JSONObject().put("topic",topic).put("resource",saved.getJSONObject("resource")).toString(),Charsets.UTF_8)
     java.nio.file.Files.move(temporary.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    },{showNotice("Resumen guardado. Puedes abrirlo desde Resúmenes descargados incluso sin iniciar sesión.",RupiUi.Tone.OK)})
   }
   button("Reportar contenido incorrecto", kind=RupiUi.Kind.DANGER){
    val reason=EditText(this).apply{hint="¿Qué debemos revisar?";minHeight=dp(48);filters=arrayOf(android.text.InputFilter.LengthFilter(500))}
    AlertDialog.Builder(this).setTitle("Revisar resumen").setView(reason).setNegativeButton("Cancelar",null).setPositiveButton("Enviar"){_,_->
     if(reason.text.isNotBlank())execute({api!!.request(summaryPath(topic)+"/"+resource.getString("id")+"/report","POST",JSONObject().put("reason",reason.text.toString()))},{loadSummary(topic)})
    }.show()
   }
  }
 }
}
