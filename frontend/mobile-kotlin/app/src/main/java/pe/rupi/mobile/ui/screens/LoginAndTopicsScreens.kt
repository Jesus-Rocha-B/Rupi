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
		andeanWelcomeBanner(
			tag = "PERÚ · PRIMARIA",
			heading = "¡Hola! Soy Rupi",
			speech = "Tu compañero de aventuras. Escribe tus datos del colegio para entrar a tu aula.",
			compact = true
		)
		lateinit var user: EditText
		lateinit var password: EditText
		var server: EditText? = null

		val defaultServer = if (BuildConfig.DEBUG) "http://10.0.2.2:8081" else "https://api.rupi.pe"

		stickerCard {
			text("Tu cuenta escolar", 20)
			muted("Pídele tu usuario a tu profe si aún no lo recuerdas.")
			user = input("Usuario escolar", hintText = "Ej: estudiante.demo")
			password = input("Contraseña", password = true, hintText = "Tu clave del colegio")
			button("¡Entrar a aprender!", kind = RupiUi.Kind.PRIMARY, iconRes = pe.rupi.mobile.R.drawable.ic_check) {
				val serverText = server?.text?.toString()?.trim()?.ifBlank { defaultServer } ?: defaultServer
				val username = user.text.toString().trim()
				val secret = password.text.toString()
				password.setText("")
				onLogin(serverText, username, secret)
			}
			muted("¿Olvidaste tu contraseña? Pídesela a tu profesor o profesora.")
		}

		// Panel de configuración avanzada de servidor (oculto por defecto para no confundir a los niños)
		val serverContainer = LinearLayout(scaffold.currentContainer().context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = android.view.View.GONE
			setPadding(scaffold.dp(4), scaffold.dp(6), scaffold.dp(4), scaffold.dp(6))
		}
		scaffold.into(serverContainer) {
			text("Ajustes de conexión (Avanzado)", 15)
			muted("Dirección del servidor escolar para pruebas o red local:")
			server = input("Servidor Rupi", hintText = defaultServer).apply {
				setText(defaultServer)
			}
		}
		scaffold.addView(serverContainer, LinearLayout.LayoutParams(-1, -2))

		button("⚙️ Ajustes de conexión (opcional)", kind = RupiUi.Kind.QUIET) {
			val isVisible = serverContainer.visibility == android.view.View.VISIBLE
			serverContainer.visibility = if (isVisible) android.view.View.GONE else android.view.View.VISIBLE
		}

		section("Estudio sin internet")
		button("Ver resúmenes descargados", kind = RupiUi.Kind.STICKER, iconRes = pe.rupi.mobile.R.drawable.ic_star) {
			onOpenSavedSummaries()
		}
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
  heroCard {
   chip("MI DÍA EN RUPI", RupiUi.LEAF_SOFT, RupiUi.SOFT_LINE, RupiUi.FOREST)
   text("Hoy", 24)
   muted("Un reto nuevo cada día, tu meta de paradas y tus logros.")
   button("Reto del día") { onOpenDailyChallenge() }
   button("Mi meta de hoy", kind = RupiUi.Kind.LEAF) { onOpenDailyGoal() }
   button("Mis logros y mi aula", kind = RupiUi.Kind.SKY) { onOpenAchievements() }
  }
  mascotGuide(
   eyebrow = "Guía Rupi",
   message = "Elige una ruta de aprendizaje abajo o completa tu reto de hoy para ganar más experiencia."
  )
  section("Mis rutas", "Tu camino de aprendizaje, como en la web. Entra a una ruta para seguir con tus paradas.")
  routesContainer = LinearLayout(scaffold.currentContainer().context).apply { orientation = LinearLayout.VERTICAL }
  addView(routesContainer, LinearLayout.LayoutParams(-1, -2))

  card {
   text("¿Tu profe te dio un código?", 20)
   val code = input("Código de clase")
   code.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
   code.filters = arrayOf(InputFilter.LengthFilter(40), InputFilter.AllCaps())
   button("Unirme a la ruta", kind = RupiUi.Kind.LEAF) {
    val rawCode = code.text.toString().trim()
    if (rawCode.isNotBlank()) onJoinByCode(rawCode)
   }
  }
  section("Más opciones")
  button("Mi ritmo de estudio y avisos", kind = RupiUi.Kind.LEAF) { onOpenNotifications() }
  button("Resúmenes descargados", kind = RupiUi.Kind.STICKER) { onOpenSavedSummaries() }
  button("Actualizar", kind = RupiUi.Kind.QUIET) { onRefresh() }
  button("Salir de mi cuenta", kind = RupiUi.Kind.DANGER) { onLogout() }
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
    completed = route.isCompleted
   ) { onOpenRoute(route) }
  }
 }
}
