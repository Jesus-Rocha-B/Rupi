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
  window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  registerReceiver(summaryConnectionReceiver,android.content.IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION))
  if(api==null)api=StudyAlerts.restore(this)
  if (api == null) loginScreen() else {
   val topic = state?.getString("topic")
   if (topic != null) { if(state?.getBoolean("summary")==true) loadSummary(JSONObject(topic)) else if(state?.getBoolean("cards")==true) loadCards(JSONObject(topic)) else loadTopic(JSONObject(topic)) } else if(intent.getBooleanExtra("notifications",false))notifications() else topics()
  }
 }
 override fun onSaveInstanceState(out: Bundle) { currentTopic?.let { out.putString("topic", it.toString()) }; out.putBoolean("summary",summaryMode);out.putBoolean("cards",cardMode); super.onSaveInstanceState(out) }
 override fun onDestroy() { unregisterReceiver(summaryConnectionReceiver);alive = false; worker.shutdownNow(); super.onDestroy() }
 private fun page(title: String) {
  screen++
  val scroll = ScrollView(this)
  scroll.setBackgroundColor(Color.rgb(250, 248, 245))
  scroll.setPadding(dp(16), dp(32), dp(16), dp(24))
  scroll.isFillViewport = true
  layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; fitsSystemWindows = true }
  scroll.addView(layout, android.widget.FrameLayout.LayoutParams(-1,-2))
  setContentView(scroll)
  if(android.os.Build.VERSION.SDK_INT>=30) layout.setOnApplyWindowInsetsListener { view,insets -> val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());view.setPadding(0,bars.top,0,bars.bottom);insets }
  text("rupi · $title", 24)
  notice = TextView(this).apply { setTextColor(Color.rgb(23,62,48)); setPadding(0,dp(12),0,dp(12)) }
  layout.addView(notice)
 }
 private fun text(value: String, size: Int = 18) {
  layout.addView(TextView(this).apply { text=value; textSize=size.toFloat(); setTextColor(Color.rgb(23,62,48)); setPadding(0,dp(10),0,dp(10)) })
 }
 private fun input(label: String, password: Boolean = false): EditText {
  text(label)
  val field = EditText(this).apply {
   hint=label; contentDescription=label; minHeight=dp(48); isSaveEnabled=false
   inputType=if(password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
  }
  layout.addView(field); return field
 }
 private fun button(label: String, enabled: Boolean = true, action: () -> Unit): Button {
  val button = Button(this).apply {
   text=label; setAllCaps(false); minHeight=dp(48); isEnabled=enabled
   setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(49,114,75))
   setOnClickListener { if(!busy) action() }
  }
  button.isSingleLine=false;button.maxLines=Int.MAX_VALUE;button.setHorizontallyScrolling(false);button.minWidth=dp(48);button.setPadding(dp(16),dp(12),dp(16),dp(12))
  layout.addView(button, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(12); bottomMargin=dp(8) })
  return button
 }
 private fun <T> execute(job: () -> T, done: (T) -> Unit) {
  if(busy) return
  busy=true
  val ticket=screen
  notice.text="Un momento, estamos consultando Rupi…"
  worker.execute {
   try {
    val result=job()
    runOnUiThread { if(alive) { busy=false; if(ticket==screen) { notice.text=""; done(result) } } }
   } catch(e: Exception) {
    runOnUiThread { if(alive) { busy=false; if(e is RupiSessionExpired){StudyAlerts.disable(this);api=null;loginScreen();notice.text="Vuelve a iniciar sesión.";return@runOnUiThread}; if(ticket==screen) notice.text=e.message ?: "No pudimos continuar. Revisa tu conexión." } }
   }
  }
 }
 private fun loginScreen() {
  currentTopic=null
  page("Entra a tu aula")
  button("Resúmenes descargados") {savedSummaries()}
  val server=input("Servidor Rupi (HTTPS)")
  if(BuildConfig.DEBUG) server.setText("http://10.0.2.2:8081")
  val user=input("Usuario escolar")
  val password=input("Contraseña",true)
  button("Entrar") {
   val serverText=server.text.toString();val username=user.text.toString();val secret=password.text.toString()
   password.setText("")
   execute({ val client=RupiApi(serverText);client.login(username,secret);client },{ StudyAlerts.disable(this);api=it; topics() })
  }
 }
 private fun topics() {
  currentTopic=null
  page("Mis temas")
  button("Resúmenes descargados") {savedSummaries()}
  button("Mi ritmo de estudio y avisos"){notifications()}
  button("Actualizar temas") { topics() }
  button("Salir") { StudyAlerts.disable(this);execute({ api?.request("/api/v1/auth/logout","POST") },{ api?.clear();api=null;loginScreen() }) }
  execute({ JSONArray(api!!.request("/api/v1/student/ai-quizzes/topics")) },{ items ->
   if(items.length()==0) text("Todavía no tienes paradas disponibles. Únete a una ruta en Rupi y vuelve a consultar.")
   for(i in 0 until items.length()) { val item=items.getJSONObject(i); button(item.getString("routeTitle")+" · "+item.getString("title")) { loadTopic(item) } }
  })
 }

 private fun notifications() {
  summaryMode=false;cardMode=false;currentTopic=null
  page("Mi ritmo de estudio")
  button("Volver a mis temas"){topics()}
  execute({
   Pair(JSONObject(api!!.request("/api/v1/student/notifications/preferences")),JSONObject(api!!.request("/api/v1/student/notifications")))
  },{ (status,inbox) ->
   val prefs=status.getJSONObject("preferences")
   text("Metas sin cambiar tus notas · horas de Perú")
   text("Completaste "+status.getInt("completedToday")+" actividades hoy.")
   text("Mi meta diaria")
   val goal=Spinner(this).apply{
    minimumHeight=dp(48);contentDescription="Mi meta diaria"
    adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,(1..5).map{"$it actividad(es)"})
    setSelection(prefs.getInt("dailyGoal")-1)
   };layout.addView(goal)
   fun toggle(label:String,value:Boolean)=CheckBox(this).apply{
    text=label;isChecked=value;minHeight=dp(48);setSingleLine(false);setHorizontallyScrolling(false);setPadding(dp(8),dp(12),dp(8),dp(12));this@MainActivity.layout.addView(this)
   }
   val goals=toggle("Recordar mi meta pendiente",prefs.getBoolean("goals"))
   val evaluations=toggle("Recordar evaluaciones próximas",prefs.getBoolean("evaluations"))
   val system=toggle("Permitir avisos en Android",prefs.getBoolean("system"))
   val at=input("Hora del recordatorio (HH:mm)").apply{setText(prefs.getString("remindAt"))}
   val from=input("Descansar desde (HH:mm)").apply{setText(prefs.getString("quietFrom"))}
   val until=input("Volver a recibir avisos (HH:mm)").apply{setText(prefs.getString("quietUntil"))}
   button("Guardar mis preferencias"){
    val value=JSONObject().put("dailyGoal",goal.selectedItemPosition+1).put("goals",goals.isChecked)
     .put("evaluations",evaluations.isChecked).put("system",system.isChecked)
     .put("remindAt",at.text.toString()).put("quietFrom",from.text.toString()).put("quietUntil",until.text.toString()).put("revision",prefs.getLong("revision"))
    execute({JSONObject(api!!.request("/api/v1/student/notifications/preferences","PUT",value))},{
     prefs.put("revision",it.getJSONObject("preferences").getLong("revision"))
     if(it.getJSONObject("preferences").getBoolean("system")){
      if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
       requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),25)
      else activateAlerts()
     }else{StudyAlerts.disable(this);notice.text="Preferencias guardadas. Avisos del dispositivo desactivados."}
    })
   }
   button("Recargar preferencias guardadas"){notifications()}
   text("Android consulta periódicamente con conexión. El ahorro de batería puede retrasar los avisos. Si cierras la sesión, se detienen.")
   text("Mis avisos",22)
   if(inbox.optBoolean("quiet"))text("Horario de descanso: los avisos del dispositivo están pausados.")
   val items=inbox.getJSONArray("items")
   if(items.length()==0)text("No tienes recordatorios pendientes.")
   for(i in 0 until items.length()){
    val item=items.getJSONObject(i);text(item.getString("title"),20);text(item.getString("body"))
    if(item.getBoolean("read"))text("Leído") else button("Marcar como leído"){
     execute({api!!.request("/api/v1/student/notifications/"+item.getString("id")+"/read","PUT")},{notifications()})
    }
   }
  })
 }
 private fun activateAlerts(){
  if(!StudyAlerts.allowed(this)){notice.text="Preferencias guardadas. Permite las notificaciones en los ajustes de Android para recibir avisos.";return}
  execute({StudyAlerts.enable(this,api!!)},{notice.text="Preferencias guardadas. Avisos activados en este dispositivo."})
 }
 override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,results:IntArray){
  super.onRequestPermissionsResult(code,permissions,results)
  if(code==25 && api!=null){
   if(results.isNotEmpty()&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)activateAlerts()
   else notice.text="Puedes consultar tus avisos aquí aunque no concedas el permiso de Android."
  }
 }

 private fun path(topic: JSONObject) = "/api/v1/student/learning-routes/"+topic.getString("routeId")+"/nodes/"+topic.getString("nodeId")+"/ai-quiz"
 private fun loadTopic(topic: JSONObject) {
  summaryMode=false;cardMode=false
  currentTopic=topic
  page(topic.getString("title"))
  button("Volver a mis temas") { topics() }
  button("Consultar mi práctica") { loadTopic(topic) }
  button("Ver resumen visual") {loadSummary(topic)}
  button("Repasar con tarjetas") { loadCards(topic) }
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
  for(i in 0 until questions.length()) {
   val q=questions.getJSONObject(i)
   text(q.getString("text"),20)
   val group=RadioGroup(this)
   val options=q.getJSONArray("options")
   val ids=mutableMapOf<Int,String>()
   val answered=!q.isNull("selectedOption")
   for(j in 0 until options.length()) {
    val option=options.getJSONObject(j)
    val radio=RadioButton(this).apply { id=j+1;text=option.getString("text");minHeight=dp(48);minWidth=dp(48);setSingleLine(false);setHorizontallyScrolling(false);setPadding(dp(12),dp(12),dp(12),dp(12));isEnabled=!answered;setTextColor(Color.rgb(23,62,48)) }
    ids[j+1]=option.getString("id");group.addView(radio)
    if(answered&&option.getString("id")==q.optString("selectedOption")) group.check(j+1)
   }
   layout.addView(group)
   if(answered) text((if(q.optBoolean("correct")) "¡Buen trabajo! " else "Sigamos aprendiendo. ")+q.optString("explanation"))
   else {
    val submit=button("Comprobar mi respuesta",false) {
     val selected=ids[group.checkedRadioButtonId] ?: return@button
     execute({ api!!.request("/api/v1/student/ai-quizzes/"+view.getString("practiceId")+"/answers","POST",
      JSONObject().put("questionId",q.getString("id")).put("optionId",selected)) },{ loadTopic(topic) })
    }
    group.setOnCheckedChangeListener { _,checked -> submit.isEnabled=ids.containsKey(checked) }
   }
   button("Reportar esta pregunta") {
    val reason=EditText(this).apply { hint="¿Qué debemos revisar?";minHeight=dp(48);filters=arrayOf(android.text.InputFilter.LengthFilter(500)) }
    AlertDialog.Builder(this).setTitle("Revisar pregunta").setView(reason).setNegativeButton("Cancelar",null)
     .setPositiveButton("Enviar") { _,_ ->
      if(reason.text.isNotBlank()) execute({ api!!.request("/api/v1/student/ai-quizzes/"+view.getString("practiceId")+"/reports","POST",
       JSONObject().put("questionId",q.getString("id")).put("reason",reason.text.toString())) },{ loadTopic(topic) })
     }.show()
   }
  }
  if(state=="COMPLETADO") text("¡Terminaste! "+view.getInt("correct")+" de "+questions.length()+" aciertos. Revisa las explicaciones.")
 }

 private fun cardsPath(topic: JSONObject) = path(topic).removeSuffix("ai-quiz")+"flashcards"
 private fun loadCards(topic: JSONObject) {
  currentTopic=topic
  cardMode=true
  page("Repasar con tarjetas")
  button("Volver al tema") { cardMode=false;loadTopic(topic) }
  button("Recuperar mi repaso") { loadCards(topic) }
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
  button(if(revealed) "Ver pregunta" else "Ver respuesta") {command(if(revealed)"NAVIGATE" else "REVEAL")}
  button("Anterior",index>0) {move(-1)}
  button("Siguiente",index<cards.length()-1) {move(1)}
  if(revealed) {
   button("Lo recuerdo") {command("REMEMBER")}
   button("Necesito practicar") {command("PRACTICE")}
  }
  text("Fuente indicada por tu docente: "+card.getString("source"))
  text("Primero repasamos lo que necesita práctica; después las tarjetas nuevas y lo que recuerdas.")
  button("Volver a repasar") {command("RESTART")}
 }

 private fun summaryPath(topic: JSONObject)=path(topic).removeSuffix("ai-quiz")+"visual-summary"
 private fun summaryFile(topic: JSONObject): java.io.File {
  val folder=java.io.File(filesDir,"visual-summaries");folder.mkdirs()
  return java.io.File(folder,java.util.UUID.fromString(topic.getString("routeId")).toString()+"-"+java.util.UUID.fromString(topic.getString("nodeId")).toString()+".json")
 }
 private fun savedSummaries() {
  summaryMode=false;currentTopic=null
  page("Resúmenes descargados")
  text("Copias guardadas en este dispositivo. Pueden estar desactualizadas; vuelve al tema con conexión para comprobar su vigencia.")
  button("Volver") {if(api==null)loginScreen() else topics()}
  val files=java.io.File(filesDir,"visual-summaries").listFiles().orEmpty()
  if(files.isEmpty())text("Todavía no guardaste resúmenes.")
  files.forEach { file ->
   try {
    val saved=JSONObject(file.readText(Charsets.UTF_8));val topic=saved.getJSONObject("topic")
    button(topic.getString("title")) {
     currentTopic=topic;summaryMode=true;page("Copia descargada")
     button("Volver a mis descargas") {savedSummaries()}
     if(api!=null)button("Comprobar vigencia") {loadSummary(topic)}
     renderSummary(topic,saved.getJSONObject("resource"),true)
    }
   }catch(_:Exception){text("No pudimos leer una descarga.")}
  }
 }
 private fun loadSummary(topic: JSONObject) {
  summaryMode=true;cardMode=false;currentTopic=topic
  page("Ver resumen visual")
  button("Volver al tema") {summaryMode=false;loadTopic(topic)}
  button("Comprobar versión y revisión") {loadSummary(topic)}
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
   button("Guardar para consultar sin conexión"){
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
    },{notice.text="Resumen guardado. Puedes abrirlo desde Resúmenes descargados incluso sin iniciar sesión."})
   }
   button("Reportar contenido incorrecto"){
    val reason=EditText(this).apply{hint="¿Qué debemos revisar?";minHeight=dp(48);filters=arrayOf(android.text.InputFilter.LengthFilter(500))}
    AlertDialog.Builder(this).setTitle("Revisar resumen").setView(reason).setNegativeButton("Cancelar",null).setPositiveButton("Enviar"){_,_->
     if(reason.text.isNotBlank())execute({api!!.request(summaryPath(topic)+"/"+resource.getString("id")+"/report","POST",JSONObject().put("reason",reason.text.toString()))},{loadSummary(topic)})
    }.show()
   }
  }
 }
}
