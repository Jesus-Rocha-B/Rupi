package pe.rupi.mobile.ui.screens

import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import pe.rupi.mobile.BuildConfig
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.data.model.LearningRouteSummary
import pe.rupi.mobile.ui.components.ScreenScaffold
import pe.rupi.mobile.ui.state.TopicsUiState

fun mountLoginScreen(
 scaffold: ScreenScaffold,
 onLogin: (serverUrl: String, username: String, password: String) -> Unit,
 onOpenSavedSummaries: () -> Unit
) {
 scaffold.mountPage("Entra a tu aula")
 scaffold.rebuildBody(preserveScroll = false) {
  lateinit var server: EditText
  lateinit var user: EditText
  lateinit var password: EditText
  card {
   text("¡Hola! ¿Listo para aprender?", 22)
   muted("Usa el usuario y la contraseña que te dio tu profe.")
   server = input("Servidor Rupi (HTTPS)")
   if (BuildConfig.DEBUG) server.setText("http://10.0.2.2:8081")
   user = input("Usuario escolar")
   password = input("Contraseña", password = true)
   button("Entrar") {
    val serverText = server.text.toString()
    val username = user.text.toString()
    val secret = password.text.toString()
    password.setText("")
    onLogin(serverText, username, secret)
   }
  }
  section("Sin conexión")
  button("Resúmenes descargados", kind = RupiUi.Kind.QUIET) { onOpenSavedSummaries() }
 }
}

/**
 * Construye el esqueleto fijo de "Mis temas" y retorna el contenedor donde se inyectan las rutas al llegar.
 */
fun mountTopicsScreen(
 scaffold: ScreenScaffold,
 onOpenDailyChallenge: () -> Unit,
 onOpenDailyGoal: () -> Unit,
 onOpenAchievements: () -> Unit,
 onJoinByCode: (String) -> Unit,
 onOpenNotifications: () -> Unit,
 onOpenSavedSummaries: () -> Unit,
 onRefresh: () -> Unit,
 onLogout: () -> Unit
): LinearLayout {
 scaffold.mountPage("Mis temas")
 lateinit var routesContainer: LinearLayout
 scaffold.rebuildBody(preserveScroll = false) {
  card {
   text("Hoy", 22)
   muted("Un reto nuevo cada día y tu meta de paradas.")
   button("Reto del día") { onOpenDailyChallenge() }
   button("Mi meta de hoy", kind = RupiUi.Kind.LEAF) { onOpenDailyGoal() }
   button("Mis logros y mi aula", kind = RupiUi.Kind.SKY) { onOpenAchievements() }
  }
  section("Mis rutas", "Tu camino de aprendizaje, como en la web. Entra a una ruta para seguir con tus paradas.")
  routesContainer = LinearLayout(scaffold.currentContainer().context).apply { orientation = LinearLayout.VERTICAL }
  addView(routesContainer, LinearLayout.LayoutParams(-1, -2))

  card {
   text("¿Tu profe te dio un código?", 20)
   val code = input("Código de clase")
   code.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
   code.filters = arrayOf(InputFilter.LengthFilter(40), InputFilter.AllCaps())
   button("Unirme a la ruta", kind = RupiUi.Kind.LEAF) {
    onJoinByCode(code.text.toString())
   }
  }
  section("Más")
  button("Mi ritmo de estudio y avisos", kind = RupiUi.Kind.LEAF) { onOpenNotifications() }
  button("Resúmenes descargados", kind = RupiUi.Kind.QUIET) { onOpenSavedSummaries() }
  button("Actualizar", kind = RupiUi.Kind.QUIET) { onRefresh() }
  button("Salir", kind = RupiUi.Kind.DANGER) { onLogout() }
 }
 return routesContainer
}

fun renderTopicsRoutesList(
 scaffold: ScreenScaffold,
 routesContainer: LinearLayout,
 state: TopicsUiState,
 onOpenRoute: (LearningRouteSummary) -> Unit
) {
 routesContainer.removeAllViews()
 if (!state.routesLoaded) return
 scaffold.into(routesContainer) {
  if (state.routes.isEmpty()) {
   card { muted("Todavía no estás en ninguna ruta. Pídele a tu profe el código de clase para unirte.") }
  }
  state.routes.forEach { route ->
   tile(
    title = route.title,
    detail = route.subtitleText(),
    mark = if (route.isCompleted) "✓" else "›"
   ) { onOpenRoute(route) }
  }
 }
}
