package pe.rupi.mobile.ui.screens

import android.graphics.Color
import android.os.Build
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import pe.rupi.mobile.BuildConfig
import pe.rupi.mobile.R
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.data.model.LearningRouteSummary
import pe.rupi.mobile.ui.components.ScreenScaffold
import pe.rupi.mobile.ui.state.TopicsUiState

private fun isEmulator(): Boolean {
	val fp = Build.FINGERPRINT.lowercase()
	val model = Build.MODEL.lowercase()
	val hw = Build.HARDWARE.lowercase()
	val prod = Build.PRODUCT.lowercase()
	return fp.startsWith("generic") || fp.startsWith("unknown") ||
		model.contains("google_sdk") || model.contains("emulator") ||
		model.contains("android sdk") || hw.contains("goldfish") ||
		hw.contains("ranchu") || prod.contains("sdk") || prod.contains("google_sdk")
}

fun mountLoginScreen(
	scaffold: ScreenScaffold,
	onLogin: (serverUrl: String, username: String, password: String) -> Unit,
	onOpenSavedSummaries: () -> Unit
) {
	scaffold.mountPage("Entra a tu aula")
	scaffold.rebuildBody(preserveScroll = false) {
		val context = scaffold.currentContainer().context
		val dp = { n: Int -> scaffold.dp(n) }

		val defaultServer = if (BuildConfig.DEBUG) {
			if (isEmulator()) "http://10.0.2.2:8081" else "http://127.0.0.1:8081"
		} else {
			"https://api.rupi.pe"
		}

		andeanWelcomeBanner(
			tag = "PERÚ · PRIMARIA",
			heading = "¡Hola! Soy Rupi",
			speech = "Tu compañero de aventuras escolares. Escribe tus datos para entrar a tu aula.",
			compact = true
		)

		lateinit var user: EditText
		lateinit var password: EditText
		var server: EditText? = null

		stickerCard {
			text("Entra a tu aula", 20)
			muted("Escribe tus datos o pulsa el botón rápido de Mateo para probar:")
			user = input("Usuario escolar", hintText = "Ej: estudiante.demo")
			password = input("Contraseña", password = true, hintText = "Tu clave del colegio")

			button("¡Entrar a aprender! 🚀", kind = RupiUi.Kind.PRIMARY, iconRes = R.drawable.ic_check) {
				val serverText = server?.text?.toString()?.trim()?.ifBlank { defaultServer } ?: defaultServer
				val username = user.text.toString().trim()
				val secret = password.text.toString()
				password.setText("")
				onLogin(serverText, username, secret)
			}

			// Botón de 1-toque para pruebas y demostración rápida
			button("⚡ Entrar como Mateo (Demo)", kind = RupiUi.Kind.STICKER, iconRes = R.drawable.ic_star) {
				user.setText("estudiante.demo")
				password.setText("123456")
				val serverText = server?.text?.toString()?.trim()?.ifBlank { defaultServer } ?: defaultServer
				onLogin(serverText, "estudiante.demo", "123456")
			}

			muted("¿Olvidaste tu contraseña? Pídesela a tu profesor o profesora.")
		}

		button("📦 Ver resúmenes guardados (sin internet)", kind = RupiUi.Kind.QUIET, iconRes = R.drawable.ic_star) {
			onOpenSavedSummaries()
		}

		// Panel de configuración de conexión móvil (plegable con presets táctiles)
		val serverContainer = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
			setPadding(dp(8), dp(6), dp(8), dp(6))
		}
		scaffold.into(serverContainer) {
			text("Conexión con el servidor", 16)
			muted("Selecciona tu entorno de red:")

			val presetsRow = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(0, dp(4), 0, dp(8))
			}
			val pParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) }

			fun presetButton(label: String, url: String): Button {
				return Button(context).apply {
					text = label
					isAllCaps = false
					minHeight = dp(42)
					textSize = 12f
					typeface = RupiUi.fontExtraBold(context)
					background = RupiUi.tactile(context, RupiUi.Kind.QUIET, radiusDp = 10)
					setTextColor(RupiUi.FOREST)
					setPadding(dp(4), dp(4), dp(4), dp(6))
					setOnClickListener {
						server?.setText(url)
					}
				}
			}

			presetsRow.addView(presetButton("📱 USB\n(Local)", "http://127.0.0.1:8081"), pParams)
			presetsRow.addView(presetButton("💻 Emulador\n(10.0.2.2)", "http://10.0.2.2:8081"), pParams)
			presetsRow.addView(presetButton("📶 Wi-Fi\n(Red PC)", "http://10.200.174.1:8081"), pParams)
			presetsRow.addView(presetButton("☁️ Nube\n(api.rupi.pe)", "https://api.rupi.pe"), pParams)
			addView(presetsRow, LinearLayout.LayoutParams(-1, -2))

			server = input("URL del servidor", hintText = defaultServer).apply {
				setText(defaultServer)
			}
		}
		scaffold.addView(serverContainer, LinearLayout.LayoutParams(-1, -2))

		button("⚙️ Ajustes de conexión de red", kind = RupiUi.Kind.QUIET) {
			val isVisible = serverContainer.visibility == View.VISIBLE
			serverContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
		}
	}
}

/**
 * Pantalla principal móvil "Mis temas" con navegación por pestañas táctiles:
 * 🗺️ Rutas | ⚡ Retos | 🎒 Mi Aula
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
		val context = scaffold.currentContainer().context
		val dp = { n: Int -> scaffold.dp(n) }

		mascotGuide(
			eyebrow = "¡Hola, explorador!",
			message = "Elige tu ruta para avanzar con tus paradas o revisa tus retos y logros."
		)

		// Barra de navegación segmentada (Pestañas móviles nativas)
		val tabsBar = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER
			setPadding(0, dp(4), 0, dp(8))
		}

		val routesSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.VISIBLE
		}
		val challengesSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}
		val classroomSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}

		fun selectTab(index: Int) {
			routesSection.visibility = if (index == 0) View.VISIBLE else View.GONE
			challengesSection.visibility = if (index == 1) View.VISIBLE else View.GONE
			classroomSection.visibility = if (index == 2) View.VISIBLE else View.GONE
			for (i in 0 until tabsBar.childCount) {
				val btn = tabsBar.getChildAt(i) as? Button ?: continue
				val selected = (i == index)
				btn.background = if (selected) {
					RupiUi.tactile(context, RupiUi.Kind.PRIMARY, radiusDp = 14)
				} else {
					RupiUi.tactile(context, RupiUi.Kind.QUIET, radiusDp = 14)
				}
				btn.setTextColor(if (selected) Color.WHITE else RupiUi.FOREST)
			}
		}

		fun createTabButton(title: String, index: Int, isInitial: Boolean): Button {
			return Button(context).apply {
				text = title
				isAllCaps = false
				minHeight = dp(46)
				typeface = RupiUi.fontExtraBold(context)
				textSize = 14f
				stateListAnimator = null
				elevation = 0f
				setPadding(dp(10), dp(8), dp(10), dp(10))
				background = if (isInitial) {
					RupiUi.tactile(context, RupiUi.Kind.PRIMARY, radiusDp = 14)
				} else {
					RupiUi.tactile(context, RupiUi.Kind.QUIET, radiusDp = 14)
				}
				setTextColor(if (isInitial) Color.WHITE else RupiUi.FOREST)
				setOnClickListener { selectTab(index) }
			}
		}

		val tabParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
			setMargins(dp(3), 0, dp(3), 0)
		}
		tabsBar.addView(createTabButton("🗺️ Rutas", 0, true), tabParams)
		tabsBar.addView(createTabButton("⚡ Retos", 1, false), tabParams)
		tabsBar.addView(createTabButton("🎒 Mi Aula", 2, false), tabParams)
		addView(tabsBar, LinearLayout.LayoutParams(-1, -2))

		// 1. PESTAÑA: MIS RUTAS
		scaffold.into(routesSection) {
			section("Mis rutas escolares", "Elige una ruta para seguir con tus paradas.")
			routesContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			addView(routesContainer, LinearLayout.LayoutParams(-1, -2))
			button("🔄 Actualizar rutas", kind = RupiUi.Kind.QUIET) { onRefresh() }
		}
		addView(routesSection, LinearLayout.LayoutParams(-1, -2))

		// 2. PESTAÑA: RETOS & METAS
		scaffold.into(challengesSection) {
			heroCard {
				chip("ACTIVIDAD DIARIA", RupiUi.LEAF_SOFT, RupiUi.SOFT_LINE, RupiUi.FOREST, iconRes = R.drawable.ic_flame)
				text("Retos y Metas de Hoy", 22)
				muted("Un reto nuevo cada día para ganar puntos de experiencia y trofeos.")
				button("Reto del día", kind = RupiUi.Kind.PRIMARY, iconRes = R.drawable.ic_star) { onOpenDailyChallenge() }
				button("Mi meta de hoy", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) { onOpenDailyGoal() }
				button("Mis logros y trofeos", kind = RupiUi.Kind.SKY, iconRes = R.drawable.ic_award) { onOpenAchievements() }
			}
		}
		addView(challengesSection, LinearLayout.LayoutParams(-1, -2))

		// 3. PESTAÑA: MI AULA & AJUSTES
		scaffold.into(classroomSection) {
			card {
				text("¿Tu profe te dio un código?", 20)
				muted("Ingresa el código para sumarte a tu nueva clase:")
				val code = input("Código de clase", hintText = "Ej: AYACUCHO2026")
				code.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
				code.filters = arrayOf(InputFilter.LengthFilter(40), InputFilter.AllCaps())
				button("Unirme a la ruta", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) {
					val rawCode = code.text.toString().trim()
					if (rawCode.isNotBlank()) onJoinByCode(rawCode)
				}
			}
			section("Herramientas escolares")
			button("🔔 Mi ritmo de estudio y avisos", kind = RupiUi.Kind.LEAF) { onOpenNotifications() }
			button("📦 Resúmenes descargados", kind = RupiUi.Kind.STICKER, iconRes = R.drawable.ic_star) { onOpenSavedSummaries() }
			button("Salir de mi cuenta", kind = RupiUi.Kind.DANGER) { onLogout() }
		}
		addView(classroomSection, LinearLayout.LayoutParams(-1, -2))
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
