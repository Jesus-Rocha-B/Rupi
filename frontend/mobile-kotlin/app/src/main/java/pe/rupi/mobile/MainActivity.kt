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
 private var cardMode=false
 private var currentTopic: JSONObject? = null
 private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
 override fun onCreate(state: Bundle?) {
  super.onCreate(state)
  if (api == null) loginScreen() else {
   val topic = state?.getString("topic")
   if (topic != null) { if(state?.getBoolean("cards")==true) loadCards(JSONObject(topic)) else loadTopic(JSONObject(topic)) } else topics()
  }
 }
 override fun onSaveInstanceState(out: Bundle) { currentTopic?.let { out.putString("topic", it.toString()) }; out.putBoolean("cards",cardMode); super.onSaveInstanceState(out) }
 override fun onDestroy() { alive = false; worker.shutdownNow(); super.onDestroy() }
 private fun page(title: String) {
  screen++
  val scroll = ScrollView(this)
  scroll.setBackgroundColor(Color.rgb(250, 248, 245))
  scroll.setPadding(dp(16), dp(32), dp(16), dp(24))
  scroll.isFillViewport = true
  layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; fitsSystemWindows = true }
  scroll.addView(layout)
  setContentView(scroll)
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
    runOnUiThread { if(alive) { busy=false; if(ticket==screen) notice.text=e.message ?: "No pudimos continuar. Revisa tu conexión." } }
   }
  }
 }
 private fun loginScreen() {
  currentTopic=null
  page("Entra a tu aula")
  val server=input("Servidor Rupi (HTTPS)")
  if(BuildConfig.DEBUG) server.setText("http://10.0.2.2:8081")
  val user=input("Usuario escolar")
  val password=input("Contraseña")
  button("Entrar") {
   val serverText=server.text.toString();val username=user.text.toString();val secret=password.text.toString()
   password.setText("")
   execute({ val client=RupiApi(serverText);client.login(username,secret);client },{ api=it; topics() })
  }
 }
 private fun topics() {
  currentTopic=null
  page("Mis temas")
  button("Actualizar temas") { topics() }
  button("Salir") { execute({ api?.request("/api/v1/auth/logout","POST") },{ api?.clear();api=null;loginScreen() }) }
  execute({ JSONArray(api!!.request("/api/v1/student/ai-quizzes/topics")) },{ items ->
   if(items.length()==0) text("Todavía no tienes paradas disponibles. Únete a una ruta en Rupi y vuelve a consultar.")
   for(i in 0 until items.length()) { val item=items.getJSONObject(i); button(item.getString("routeTitle")+" · "+item.getString("title")) { loadTopic(item) } }
  })
 }
 private fun path(topic: JSONObject) = "/api/v1/student/learning-routes/"+topic.getString("routeId")+"/nodes/"+topic.getString("nodeId")+"/ai-quiz"
 private fun loadTopic(topic: JSONObject) {
  cardMode=false
  currentTopic=topic
  page(topic.getString("title"))
  button("Volver a mis temas") { topics() }
  button("Consultar mi práctica") { loadTopic(topic) }
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
    val radio=RadioButton(this).apply { id=j+1;text=option.getString("text");minHeight=dp(48);isEnabled=!answered;setTextColor(Color.rgb(23,62,48)) }
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
     if(!busy&&kotlin.math.abs(dx)>dp(60)&&kotlin.math.abs(dx)>kotlin.math.abs(dy)*1.5) {move(if(dx<0)1 else -1);true} else dragged
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
}
