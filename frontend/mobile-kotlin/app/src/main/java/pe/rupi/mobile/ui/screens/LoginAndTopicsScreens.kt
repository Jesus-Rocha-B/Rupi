package pe.rupi.mobile.ui.screens

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
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

private fun generateOfflineTutorResponse(question: String): String {
	val q = question.lowercase()
	return when {
		q.contains("fraccion") || q.contains("matematica") || q.contains("sumar") || q.contains("restar") ->
			"Buena pregunta de matematicas. Piensa en una fraccion como una pizza o naranja que compartes en partes iguales. El numero de abajo (denominador) indica en cuantas partes iguales se dividio, y el de arriba (numerador) cuantas partes tomaste. Que ejercicio deseas resolver?"
		q.contains("fotosintesis") || q.contains("planta") || q.contains("ciencia") ->
			"Excelente duda de ciencias. La fotosintesis es el proceso donde las plantas toman luz solar, agua y dioxido de carbono para producir su propio alimento y devolvernos oxigeno limpio. Lo viste hoy en tu clase?"
		q.contains("inca") || q.contains("historia") || q.contains("castilla") || q.contains("peru") ->
			"Gran tema de historia de nuestro Peru. Los Incas construyeron caminos por los Andes (el Qhapaq Nan) y terrazas de cultivo (andenes). Que parte de la historia te gustaria repasar?"
		q.contains("reto") || q.contains("meta") || q.contains("consejo") || q.contains("estudiar") ->
			"Vamos paso a paso. Mi recomendacion es avanzar una parada escolar a la vez con calma. Lee con atencion cada pregunta: equivocarse es parte normal de aprender."
		else ->
			"Hola, Mateo. Como tu tutor Rupi estoy aqui para acompanarte. Cuentame sobre que leccion o tarea de tu aula deseas consultar hoy."
	}
}

/**
 * Pantalla de bienvenida, inicio de sesión y registro escolar inspirada fielmente en Duolingo:
 * mascota Rupi centrada, campos con iconos a la izquierda (usuario, correo, candado) y botones limpios sin iconos.
 */
fun mountLoginScreen(
	scaffold: ScreenScaffold,
	onLogin: (serverUrl: String, username: String, password: String) -> Unit,
	onOpenSavedSummaries: () -> Unit
) {
	scaffold.mountLoginPage()

	val defaultServer = if (BuildConfig.DEBUG) {
		if (isEmulator()) "http://10.0.2.2:8081" else "http://127.0.0.1:8081"
	} else {
		"https://api.rupi.pe"
	}

	fun renderAuthForm(isRegister: Boolean) {
		scaffold.rebuildBody(preserveScroll = false) {
			val context = scaffold.currentContainer().context
			val dp = { n: Int -> scaffold.dp(n) }

			// 1. Mascota Rupi centrada y saludo limpio en una sola línea (Estilo Duolingo)
			val heroLayout = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				gravity = Gravity.CENTER_HORIZONTAL
				setPadding(0, dp(6), 0, dp(10))
			}
			heroLayout.addView(
				ImageView(context).apply {
					setImageResource(R.drawable.ic_rupi_mascot)
					scaleType = ImageView.ScaleType.FIT_CENTER
					importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
				},
				LinearLayout.LayoutParams(dp(132), dp(138))
			)

			heroLayout.addView(TextView(context).apply {
				text = if (isRegister) "Crea tu cuenta escolar" else "Inicia sesión en tu cuenta"
				typeface = RupiUi.fontTitle(context)
				textSize = 23f
				setTextColor(RupiUi.FOREST)
				gravity = Gravity.CENTER_HORIZONTAL
				setSingleLine(true)
				maxLines = 1
				setPadding(0, dp(4), 0, 0)
			})

			heroLayout.addView(TextView(context).apply {
				text = if (isRegister) "Únete a tu aula para aprender jugando" else "Aprende jugando en tu aula escolar"
				typeface = RupiUi.fontBody(context)
				textSize = 14.5f
				setTextColor(RupiUi.MUTED)
				gravity = Gravity.CENTER_HORIZONTAL
				setSingleLine(true)
				maxLines = 1
				setPadding(dp(12), dp(2), dp(12), 0)
			})
			addView(heroLayout, LinearLayout.LayoutParams(-1, -2))

			// 2. Tarjeta de Formulario limpia con bordes suaves
			card {
				if (isRegister) {
					// REGISTRO ESCOLAR (Fiel a la imagen 3 de Duolingo)
					val nameInput = input(label = "", hintText = "Nombre completo", iconRes = R.drawable.ic_user)
					val emailInput = input(label = "", hintText = "Correo electrónico escolar", iconRes = R.drawable.ic_mail)
					val regPasswordInput = input(label = "", password = true, hintText = "Contraseña", iconRes = R.drawable.ic_lock)

					val termsBox = LinearLayout(context).apply {
						orientation = LinearLayout.HORIZONTAL
						gravity = Gravity.CENTER_VERTICAL
						setPadding(0, dp(6), 0, dp(10))
					}
					termsBox.addView(ImageView(context).apply {
						setImageResource(R.drawable.ic_check)
						val d = drawable.mutate()
						d.setTint(RupiUi.LEAF)
						setImageDrawable(d)
					}, LinearLayout.LayoutParams(dp(18), dp(18)))
					termsBox.addView(TextView(context).apply {
						text = " Acepto las normas de convivencia y estudio escolar"
						typeface = RupiUi.fontBody(context)
						textSize = 12.5f
						setTextColor(RupiUi.FOREST)
						setSingleLine(true)
						setPadding(dp(4), 0, 0, 0)
					})
					addView(termsBox)

					// Botón limpio sin iconos en el texto
					button("CREAR CUENTA", kind = RupiUi.Kind.LOGIN_ORANGE) {
						val email = emailInput.text.toString().trim()
						val pass = regPasswordInput.text.toString()
						if (email.isBlank() || pass.isBlank()) {
							scaffold.showLocalNotice("Escribe tu correo y contraseña escolar para registrarte.")
						} else {
							onLogin(defaultServer, email, pass)
						}
					}
				} else {
					// LOGIN / INICIO DE SESIÓN (Fiel a la imagen 2 de Duolingo)
					// Los campos llevan icono a la izquierda; el botón no lleva icono.
					val userInput = input(label = "", hintText = "Correo o usuario", iconRes = R.drawable.ic_user)
					val passwordInput = input(label = "", password = true, hintText = "Contraseña", iconRes = R.drawable.ic_lock)

					// Enlace ¿Olvidaste tu contraseña? alineado a la derecha
					val forgotLayout = LinearLayout(context).apply {
						orientation = LinearLayout.HORIZONTAL
						gravity = Gravity.END
						minimumHeight = dp(40)
						isClickable = true
						isFocusable = true
						setPadding(0, 0, 0, dp(8))
						setOnClickListener {
							scaffold.showLocalNotice("Pídele a tu profesor o profesora que restablezca tu clave escolar.")
						}
					}
					forgotLayout.addView(TextView(context).apply {
						text = "¿Olvidaste tu contraseña?"
						typeface = RupiUi.fontBold(context)
						textSize = 13f
						setTextColor(RupiUi.MUTED)
						setSingleLine(true)
					})
					addView(forgotLayout, LinearLayout.LayoutParams(-1, -2))

					// Botón de Ingreso limpio (sin iconos en el botón)
					button("INGRESAR", kind = RupiUi.Kind.LOGIN_ORANGE) {
						val username = userInput.text.toString().trim().ifBlank {
							if (BuildConfig.DEBUG) "estudiante.demo" else ""
						}
						val secret = passwordInput.text.toString().ifBlank {
							if (BuildConfig.DEBUG) "123456" else ""
						}
						if (username.isBlank() || secret.isBlank()) {
							scaffold.showLocalNotice("Escribe tu correo o usuario y tu contraseña para ingresar.")
						} else {
							passwordInput.setText("")
							onLogin(defaultServer, username, secret)
						}
					}
				}

				// Acción táctil para cambiar entre el acceso y el formulario de registro.
				val toggleRow = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER
					minimumHeight = dp(48)
					setPadding(0, dp(8), 0, dp(4))
				}
				toggleRow.addView(TextView(context).apply {
					text = if (isRegister) "¿Ya tienes una cuenta? " else "¿No tienes una cuenta? "
					typeface = RupiUi.fontBody(context)
					textSize = 13.5f
					setTextColor(RupiUi.MUTED)
					setSingleLine(true)
				})
				val toggleButton = Button(context).apply {
					text = if (isRegister) "Inicia sesión" else "Regístrate"
					isAllCaps = false
					minHeight = dp(42)
					minWidth = dp(48)
					textSize = 13.5f
					typeface = RupiUi.fontExtraBold(context)
					stateListAnimator = null
					background = RupiUi.tactile(context, RupiUi.Kind.STICKER, radiusDp = 12)
					setTextColor(RupiUi.tactileText(RupiUi.Kind.STICKER))
					setPadding(dp(10), dp(6), dp(10), dp(10))
					contentDescription = if (isRegister) "Volver al inicio de sesión" else "Abrir registro escolar"
					setOnClickListener { renderAuthForm(isRegister = !isRegister) }
				}
				toggleRow.addView(toggleButton, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(6) })
				addView(toggleRow, LinearLayout.LayoutParams(-1, -2))
			}

			// Botón de resúmenes sin conexión
			button("Estudiar sin conexión (Resúmenes descargados)", kind = RupiUi.Kind.QUIET) {
				onOpenSavedSummaries()
			}
		}
	}

	renderAuthForm(isRegister = false)
}

private var selectedRouteFilter: Int = 0 // 0: Rutas RUPI Escuela, 1: Rutas Comunidad
private var lastTopicsStateCache: TopicsUiState? = null
private var lastOnOpenRouteCache: ((LearningRouteSummary) -> Unit)? = null

/**
 * Pantalla principal con Top Stats Bar y Fixed Bottom Navbar estilo Duolingo:
 * Camino (Rutas) | Tutor IA (Chat) | Misiones (Retos) | Logros (Aula) | Perfil
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
	lateinit var routesContainer: LinearLayout

	// Contenedores para cada pestana
	lateinit var routesSection: LinearLayout
	lateinit var tutorSection: LinearLayout
	lateinit var questsSection: LinearLayout
	lateinit var achievementsSection: LinearLayout
	lateinit var profileSection: LinearLayout
	var refreshMissionAnimations: (() -> Unit)? = null

	fun switchTab(index: Int) {
		routesSection.visibility = if (index == 0) View.VISIBLE else View.GONE
		tutorSection.visibility = if (index == 1) View.VISIBLE else View.GONE
		questsSection.visibility = if (index == 2) View.VISIBLE else View.GONE
		achievementsSection.visibility = if (index == 3) View.VISIBLE else View.GONE
		profileSection.visibility = if (index == 4) View.VISIBLE else View.GONE
		refreshMissionAnimations?.invoke()
	}

	scaffold.mountDuolingoShell(
		streakDays = 3,
		xpPoints = 120,
		hearts = 5,
		activeTab = 0,
		hideTopBarTabs = setOf(3, 4),
		onTabSelected = { index -> switchTab(index) }
	)

	scaffold.rebuildBody(preserveScroll = false) {
		val context = scaffold.currentContainer().context
		val dp = { n: Int -> scaffold.dp(n) }

		routesSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.VISIBLE
		}
		tutorSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}
		questsSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}
		achievementsSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}
		profileSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}

		// -----------------------------------------------------------------
		// TAB 0: CAMINO DE APRENDIZAJE (Exploración escolar de Ayacucho)
		// -----------------------------------------------------------------
		scaffold.into(routesSection) {
			// Cabecera de exploración escolar con la mascota Rupi y ambientación de Ayacucho
			val exploreHero = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.ayacuchoCard(context)
				setPadding(dp(16), dp(14), dp(16), dp(16))
			}
			exploreHero.addView(
				ImageView(context).apply {
					setImageResource(R.drawable.ic_rupi_mascot)
					scaleType = ImageView.ScaleType.FIT_CENTER
					importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
				},
				LinearLayout.LayoutParams(dp(58), dp(62)).apply { rightMargin = dp(12) }
			)
			val exploreCopy = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			exploreCopy.addView(TextView(context).apply {
				text = "AYACUCHO · RUTAS ESCOLARES"
				typeface = RupiUi.fontExtraBold(context)
				textSize = 11.5f
				setTextColor(RupiUi.SUN_DARK)
			})
			exploreCopy.addView(TextView(context).apply {
				text = "¡Aprende jugando, Mateo!"
				typeface = RupiUi.fontTitle(context)
				textSize = 19f
				setTextColor(RupiUi.FOREST)
			})
			exploreCopy.addView(TextView(context).apply {
				text = "Elige una aventura escolar para avanzar en tu camino."
				typeface = RupiUi.fontBody(context)
				textSize = 13f
				setTextColor(RupiUi.MUTED)
			})
			exploreHero.addView(exploreCopy, LinearLayout.LayoutParams(0, -2, 1f))
			addView(exploreHero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

			// Filtros de navegación: Rutas de Escuela vs Rutas de la Comunidad
			val filterLayout = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding(0, 0, 0, dp(12))
			}
			val chipEscuela = TextView(context)
			val chipComunidad = TextView(context)

			fun updateFilterChips() {
				chipEscuela.apply {
					text = "Rutas RUPI · Escuela"
					typeface = if (selectedRouteFilter == 0) RupiUi.fontExtraBold(context) else RupiUi.fontBold(context)
					textSize = 12.5f
					setTextColor(if (selectedRouteFilter == 0) Color.WHITE else RupiUi.FOREST)
					background = if (selectedRouteFilter == 0) {
						RupiUi.rounded(RupiUi.FOREST, dp(16).toFloat(), dp(2), RupiUi.FOREST)
					} else {
						RupiUi.rounded(Color.WHITE, dp(16).toFloat(), dp(1), RupiUi.LINE)
					}
					setPadding(dp(14), dp(8), dp(14), dp(8))
				}
				chipComunidad.apply {
					text = "Rutas de la Comunidad"
					typeface = if (selectedRouteFilter == 1) RupiUi.fontExtraBold(context) else RupiUi.fontBold(context)
					textSize = 12.5f
					setTextColor(if (selectedRouteFilter == 1) Color.WHITE else RupiUi.FOREST)
					background = if (selectedRouteFilter == 1) {
						RupiUi.rounded(RupiUi.FOREST, dp(16).toFloat(), dp(2), RupiUi.FOREST)
					} else {
						RupiUi.rounded(Color.WHITE, dp(16).toFloat(), dp(1), RupiUi.LINE)
					}
					setPadding(dp(14), dp(8), dp(14), dp(8))
				}
			}

			chipEscuela.setOnClickListener {
				if (selectedRouteFilter != 0) {
					selectedRouteFilter = 0
					updateFilterChips()
					val s = lastTopicsStateCache
					val cb = lastOnOpenRouteCache
					if (s != null && cb != null) {
						renderTopicsRoutesList(scaffold, routesContainer, s, cb)
					}
				}
			}
			chipComunidad.setOnClickListener {
				if (selectedRouteFilter != 1) {
					selectedRouteFilter = 1
					updateFilterChips()
					val s = lastTopicsStateCache
					val cb = lastOnOpenRouteCache
					if (s != null && cb != null) {
						renderTopicsRoutesList(scaffold, routesContainer, s, cb)
					}
				}
			}
			updateFilterChips()
			filterLayout.addView(chipEscuela, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
			filterLayout.addView(chipComunidad, LinearLayout.LayoutParams(-2, -2))
			addView(filterLayout, LinearLayout.LayoutParams(-1, -2))

			// Contenedor dinámico de rutas escolares
			routesContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			addView(routesContainer, LinearLayout.LayoutParams(-1, -2))

			button("Actualizar camino escolar", kind = RupiUi.Kind.QUIET, iconRes = R.drawable.ic_star) { onRefresh() }
		}
		addView(routesSection, LinearLayout.LayoutParams(-1, -2))

		// -----------------------------------------------------------------
		// TAB 1: TUTOR IA (CHAT AUTÉNTICO CON RUPI - ESTILO SLATE / DUOLINGO)
		// -----------------------------------------------------------------
		scaffold.into(tutorSection) {
			val chatBox = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				background = RupiUi.rounded(
					Color.rgb(18, 28, 23),
					dp(20).toFloat(),
					dp(1),
					Color.rgb(36, 54, 44)
				)
				setPadding(dp(14), dp(16), dp(14), dp(14))
			}

			// CABECERA DEL CHAT (Avatar + Nombre + Estado En Línea)
			val chatHeader = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding(0, 0, 0, dp(12))
			}
			chatHeader.addView(
				ImageView(context).apply {
					setImageResource(R.drawable.ic_rupi_mascot)
					scaleType = ImageView.ScaleType.FIT_CENTER
					background = RupiUi.rounded(Color.rgb(28, 42, 34), dp(22).toFloat(), dp(1), Color.rgb(45, 68, 55))
					setPadding(dp(4), dp(4), dp(4), dp(4))
					importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
				},
				LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(10) }
			)
			val headerTexts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			headerTexts.addView(TextView(context).apply {
				text = "Tutor Rupi"
				typeface = RupiUi.fontTitle(context)
				textSize = 17.5f
				setTextColor(Color.WHITE)
			})
			val statusRow = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
			}
			statusRow.addView(View(context).apply {
				background = RupiUi.rounded(Color.rgb(74, 222, 128), dp(4).toFloat(), 0, 0)
			}, LinearLayout.LayoutParams(dp(7), dp(7)).apply { rightMargin = dp(6) })
			statusRow.addView(TextView(context).apply {
				text = "En línea · Primaria Perú"
				typeface = RupiUi.fontBody(context)
				textSize = 12f
				setTextColor(Color.rgb(156, 182, 168))
			})
			headerTexts.addView(statusRow)
			chatHeader.addView(headerTexts, LinearLayout.LayoutParams(0, -2, 1f))
			chatBox.addView(chatHeader)

			chatBox.addView(View(context).apply {
				setBackgroundColor(Color.rgb(32, 48, 39))
			}, LinearLayout.LayoutParams(-1, dp(1)).apply { bottomMargin = dp(10) })

			val chatHistoryLayout = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(0, dp(4), 0, dp(4))
			}

			val suggestionsContainer = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(0, dp(6), 0, dp(10))
			}
			suggestionsContainer.addView(TextView(context).apply {
				text = "Preguntas rápidas sugeridas:"
				typeface = RupiUi.fontExtraBold(context)
				textSize = 12f
				setTextColor(Color.rgb(180, 205, 192))
				setPadding(0, 0, 0, dp(6))
			})

			lateinit var chatInput: EditText
			lateinit var askQuestion: (String) -> Unit

			fun appendMessage(text: String, isUser: Boolean) {
				val bubbleRow = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = if (isUser) Gravity.END else Gravity.START
					setPadding(0, dp(5), 0, dp(5))
				}

				if (!isUser) {
					val botAvatar = ImageView(context).apply {
						setImageResource(R.drawable.ic_rupi_mascot)
						scaleType = ImageView.ScaleType.FIT_CENTER
						background = RupiUi.rounded(Color.rgb(28, 42, 34), dp(16).toFloat(), 0, 0)
						setPadding(dp(3), dp(3), dp(3), dp(3))
						importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
					}
					bubbleRow.addView(botAvatar, LinearLayout.LayoutParams(dp(32), dp(32)).apply {
						rightMargin = dp(8)
						topMargin = dp(2)
					})
				}

				val bubble = LinearLayout(context).apply {
					orientation = LinearLayout.VERTICAL
					background = if (isUser) {
						RupiUi.rounded(Color.WHITE, dp(16).toFloat(), 0, 0)
					} else {
						RupiUi.rounded(Color.rgb(28, 42, 34), dp(16).toFloat(), dp(1), Color.rgb(44, 66, 54))
					}
					setPadding(dp(14), dp(10), dp(14), dp(10))
				}

				bubble.addView(TextView(context).apply {
					this.text = text
					typeface = RupiUi.fontBody(context)
					textSize = 14f
					setTextColor(if (isUser) Color.rgb(24, 30, 27) else Color.rgb(243, 244, 246))
					setLineSpacing(0f, 1.25f)
				})

				val bubbleParams = LinearLayout.LayoutParams(-2, -2).apply {
					if (isUser) leftMargin = dp(36) else rightMargin = dp(28)
				}
				bubbleRow.addView(bubble, bubbleParams)

				bubbleRow.alpha = 0f
				bubbleRow.translationY = dp(8).toFloat()
				chatHistoryLayout.addView(bubbleRow)
				bubbleRow.animate().alpha(1f).translationY(0f).setDuration(160).start()
			}

			appendMessage("¡Hola, Mateo! Soy Rupi, tu tutor escolar. ¿Qué tema o ejercicio de tu clase quieres practicar hoy?", isUser = false)
			chatBox.addView(chatHistoryLayout)

			askQuestion = { qText: String ->
				if (qText.isNotBlank()) {
					suggestionsContainer.visibility = View.GONE
					appendMessage(qText, isUser = true)

					val thinking = TextView(context).apply {
						text = "Rupi está escribiendo..."
						typeface = RupiUi.fontBody(context)
						textSize = 12.5f
						setTextColor(Color.rgb(148, 175, 160))
						setPadding(dp(40), dp(4), dp(14), dp(4))
					}
					chatHistoryLayout.addView(thinking)

					Thread {
						val reply = try {
							val url = java.net.URI("http://127.0.0.1:8000/api/v1/ia/tutor/chat").toURL()
							val conn = url.openConnection() as java.net.HttpURLConnection
							conn.connectTimeout = 3000
							conn.readTimeout = 10000
							conn.requestMethod = "POST"
							conn.setRequestProperty("Content-Type", "application/json")
							conn.doOutput = true
							val body = org.json.JSONObject()
								.put("message", qText)
								.put("context", org.json.JSONObject()
									.put("currentGrade", "3.° de primaria")
									.put("currentSubject", "Primaria Peru")
									.put("roadmapTitle", "Aventura Rupi")
								)
							conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
							if (conn.responseCode in 200..299) {
								val res = conn.inputStream.bufferedReader().use { it.readText() }
								org.json.JSONObject(res).optString("reply", "")
							} else {
								generateOfflineTutorResponse(qText)
							}
						} catch (_: Exception) {
							generateOfflineTutorResponse(qText)
						}
						context.let {
							(it as? android.app.Activity)?.runOnUiThread {
								chatHistoryLayout.removeView(thinking)
								appendMessage(reply, isUser = false)
							}
						}
					}.start()
				}
			}

			val suggestions = listOf(
				"¿Cómo resuelvo fracciones de primaria?",
				"¿Qué es la fotosíntesis en ciencias?",
				"¿Quiénes fueron los Incas en el Perú?",
				"Dame un consejo para mi meta de hoy"
			)
			suggestions.forEach { prompt ->
				val btn = Button(context).apply {
					text = prompt
					isAllCaps = false
					minHeight = dp(38)
					typeface = RupiUi.fontBody(context)
					textSize = 12.5f
					background = RupiUi.rounded(Color.rgb(25, 38, 30), dp(12).toFloat(), dp(1), Color.rgb(42, 64, 52))
					setTextColor(Color.rgb(215, 235, 222))
					setPadding(dp(12), dp(6), dp(12), dp(6))
					setOnClickListener { askQuestion(prompt) }
				}
				suggestionsContainer.addView(btn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
			}
			chatBox.addView(suggestionsContainer)

			val inputBar = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.rounded(Color.rgb(24, 36, 29), dp(24).toFloat(), dp(1), Color.rgb(44, 66, 54))
				setPadding(dp(14), dp(4), dp(6), dp(4))
			}
			chatInput = EditText(context).apply {
				hint = "Escribe tu duda escolar aquí..."
				setHintTextColor(Color.rgb(130, 155, 142))
				setTextColor(Color.WHITE)
				textSize = 13.5f
				typeface = RupiUi.fontBody(context)
				background = null
				isSingleLine = true
			}
			inputBar.addView(chatInput, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })

			val sendBtn = ImageView(context).apply {
				setImageResource(R.drawable.ic_send)
				val d = drawable.mutate()
				d.setTint(Color.WHITE)
				setImageDrawable(d)
				background = RupiUi.rounded(RupiUi.ORANGE, dp(20).toFloat(), 0, 0)
				setPadding(dp(10), dp(10), dp(10), dp(10))
				isClickable = true
				isFocusable = true
				setOnClickListener {
					val message = chatInput.text.toString().trim()
					if (message.isNotBlank()) {
						chatInput.setText("")
						askQuestion(message)
					}
				}
			}
			inputBar.addView(sendBtn, LinearLayout.LayoutParams(dp(40), dp(40)))
			chatBox.addView(inputBar, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

			addView(chatBox, LinearLayout.LayoutParams(-1, -2))
		}
		addView(tutorSection, LinearLayout.LayoutParams(-1, -2))

		// -----------------------------------------------------------------
		// TAB 2: MISIONES RUPI — metas, reto del día y avance con la misma lógica.
		// -----------------------------------------------------------------
		scaffold.into(questsSection) {
			// Cabecera con paisaje andino y el gallito de las rocas Rupi como guía.
			val missionHeader = FrameLayout(context).apply {
				background = RupiUi.rounded(RupiUi.PAPER, dp(20).toFloat(), dp(2), RupiUi.LINE)
				clipToOutline = true
				minimumHeight = dp(100)
			}
			missionHeader.addView(ImageView(context).apply {
				setImageResource(R.drawable.bg_andean_scene)
				scaleType = ImageView.ScaleType.FIT_XY
				alpha = 0.32f
				importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			}, FrameLayout.LayoutParams(-1, dp(66), Gravity.BOTTOM))

			val heading = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding(dp(14), dp(10), dp(104), dp(12))
			}
			heading.addView(TextView(context).apply {
				text = "Misiones de hoy"
				typeface = RupiUi.fontTitle(context)
				textSize = 23f
				setTextColor(RupiUi.FOREST)
				if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
			})
			heading.addView(TextView(context).apply {
				text = "¡Rupi te acompaña en cada reto!"
				typeface = RupiUi.fontBody(context)
				textSize = 13f
				setTextColor(RupiUi.MUTED)
			})
			missionHeader.addView(heading, FrameLayout.LayoutParams(-1, -1))

			val mascot = ImageView(context).apply {
				setImageResource(R.drawable.ic_rupi_mascot)
				scaleType = ImageView.ScaleType.FIT_CENTER
				contentDescription = "Rupi, gallito de las rocas explorador, te saluda"
			}
			missionHeader.addView(mascot, FrameLayout.LayoutParams(dp(96), dp(98), Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply {
				rightMargin = dp(5)
			})
			val helloSpark = ImageView(context).apply {
				setImageResource(R.drawable.ic_star)
				setColorFilter(RupiUi.SUN_DARK)
				importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			}
			missionHeader.addView(helloSpark, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP or Gravity.RIGHT).apply {
				topMargin = dp(8); rightMargin = dp(80)
			})

			// Rupi saluda con un rebote suave; la animación se detiene al salir de pantalla.
			val mascotBob = ObjectAnimator.ofFloat(mascot, View.TRANSLATION_Y, 0f, -dp(4).toFloat(), 0f).apply {
				duration = 1500L
				repeatCount = ValueAnimator.INFINITE
				interpolator = AccelerateDecelerateInterpolator()
			}
			val sparkTwinkle = ObjectAnimator.ofFloat(helloSpark, View.ALPHA, 0.45f, 1f, 0.45f).apply {
				duration = 1050L
				repeatCount = ValueAnimator.INFINITE
				interpolator = AccelerateDecelerateInterpolator()
			}
			fun refreshAnimations() {
				val shouldAnimate = questsSection.visibility == View.VISIBLE && questsSection.isAttachedToWindow
				if (shouldAnimate) {
					if (!mascotBob.isStarted) mascotBob.start()
					if (!sparkTwinkle.isStarted) sparkTwinkle.start()
				} else {
					mascotBob.cancel()
					sparkTwinkle.cancel()
					mascot.translationY = 0f
					helloSpark.alpha = 1f
				}
			}
			refreshMissionAnimations = ::refreshAnimations
			questsSection.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
				override fun onViewAttachedToWindow(view: View) = refreshAnimations()
				override fun onViewDetachedFromWindow(view: View) = refreshAnimations()
			})
			questsSection.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refreshAnimations() }
			refreshAnimations()
			addView(missionHeader, LinearLayout.LayoutParams(-1, dp(104)).apply { bottomMargin = dp(10) })

			// Invitación naranja a las misiones y sus cofres.
			val questBanner = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.tactile(context, RupiUi.Kind.LOGIN_ORANGE, radiusDp = 18)
				setPadding(dp(14), dp(10), dp(14), dp(14))
			}
			questBanner.addView(ImageView(context).apply {
				setImageResource(R.drawable.ic_chest)
				scaleType = ImageView.ScaleType.FIT_CENTER
				contentDescription = "Cofre de recompensas"
			}, LinearLayout.LayoutParams(dp(54), dp(54)).apply { rightMargin = dp(12) })
			val qbTexts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			qbTexts.addView(TextView(context).apply {
				text = "¡Vamos por tus misiones!"
				typeface = RupiUi.fontTitle(context)
				textSize = 18f
				setTextColor(Color.WHITE)
			})
			qbTexts.addView(TextView(context).apply {
				text = "Completa retos y gana puntos para tu aventura."
				typeface = RupiUi.fontBody(context)
				textSize = 12.5f
				setTextColor(Color.WHITE)
				setLineSpacing(0f, 1.05f)
			})
			questBanner.addView(qbTexts, LinearLayout.LayoutParams(0, -2, 1f))
			addView(questBanner, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

			// Progreso de XP; la llamita conserva la señal visual de racha de Rupi.
			card {
				val missionRow = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
				}
				missionRow.addView(ImageView(context).apply {
					setImageResource(R.drawable.ic_star)
					setColorFilter(RupiUi.SUN_DARK)
					contentDescription = null
				}, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(7) })
				missionRow.addView(TextView(context).apply {
					text = "Misión del día"
					typeface = RupiUi.fontTitle(context)
					textSize = 17f
					setTextColor(RupiUi.FOREST)
				}, LinearLayout.LayoutParams(0, -2, 1f))
				val reward = TextView(context).apply {
					text = "Gana 20 XP"
					typeface = RupiUi.fontExtraBold(context)
					textSize = 12.5f
					setTextColor(RupiUi.FOREST)
					background = RupiUi.pill(context, RupiUi.SUN, RupiUi.SUN_DARK)
					setPadding(dp(10), dp(6), dp(10), dp(6))
				}
				missionRow.addView(reward)
				addView(missionRow, LinearLayout.LayoutParams(-1, -2))

				val progressRow = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
					setPadding(0, dp(8), 0, 0)
				}
				progressRow.addView(ImageView(context).apply {
					setImageResource(R.drawable.ic_flame)
					setColorFilter(RupiUi.ORANGE)
					contentDescription = "Racha de estudio"
				}, LinearLayout.LayoutParams(dp(24), dp(24)).apply { rightMargin = dp(7) })
				val xpBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
					max = 20
					progress = 14
					progressTintList = ColorStateList.valueOf(RupiUi.LEAF)
					progressBackgroundTintList = ColorStateList.valueOf(RupiUi.PAPER_DEEP)
					importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
				}
				progressRow.addView(xpBar, LinearLayout.LayoutParams(0, dp(14), 1f))
				progressRow.addView(TextView(context).apply {
					text = "14/20"
					typeface = RupiUi.fontExtraBold(context)
					textSize = 13f
					setTextColor(RupiUi.FOREST)
					setPadding(dp(7), 0, 0, 0)
				}, LinearLayout.LayoutParams(-2, -2))
				addView(progressRow, LinearLayout.LayoutParams(-1, -2))
				muted("14 de 20 XP completados hoy.")
			}

			// Reto escolar en amarillo maíz, con botón corto y táctil.
			stickerCard(fill = Color.rgb(255, 248, 220)) {
				val challengeTitle = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
				}
				challengeTitle.addView(ImageView(context).apply {
					setImageResource(R.drawable.ic_book)
					setColorFilter(RupiUi.FOREST)
					contentDescription = null
				}, LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(9) })
				val copy = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
				copy.addView(TextView(context).apply {
					text = "Reto diario escolar"
					typeface = RupiUi.fontTitle(context)
					textSize = 17f
					setTextColor(RupiUi.FOREST)
				})
				copy.addView(TextView(context).apply {
					text = "Acepta el reto para duplicar tus puntos."
					typeface = RupiUi.fontBody(context)
					textSize = 12.5f
					setTextColor(RupiUi.MUTED)
				})
				challengeTitle.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
				addView(challengeTitle, LinearLayout.LayoutParams(-1, -2))
				button("COMENZAR RETO", kind = RupiUi.Kind.LOGIN_ORANGE, iconRes = R.drawable.ic_star) {
					onOpenDailyChallenge()
				}
			}

			// Meta diaria con la llamita ilustrada y sus pasos visuales.
			stickerCard(fill = RupiUi.LEAF_SOFT) {
				val goalHeader = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
				}
				goalHeader.addView(ImageView(context).apply {
					setImageResource(R.drawable.ic_llama)
					scaleType = ImageView.ScaleType.FIT_CENTER
					contentDescription = "Llamita andina de Rupi"
				}, LinearLayout.LayoutParams(dp(54), dp(60)).apply { rightMargin = dp(10) })
				val goalCopy = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
				goalCopy.addView(TextView(context).apply {
					text = "Mi meta diaria de paradas"
					typeface = RupiUi.fontTitle(context)
					textSize = 17f
					setTextColor(RupiUi.FOREST)
				})
				goalCopy.addView(TextView(context).apply {
					text = "Consulta tu ritmo de avance diario."
					typeface = RupiUi.fontBody(context)
					textSize = 12.5f
					setTextColor(RupiUi.MUTED)
				})
				goalHeader.addView(goalCopy, LinearLayout.LayoutParams(0, -2, 1f))
				addView(goalHeader, LinearLayout.LayoutParams(-1, -2))

				val steps = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER
					setPadding(0, dp(8), 0, dp(2))
				}
				(0..2).forEach { index ->
					val node = TextView(context).apply {
						text = if (index < 2) "✓" else ""
						gravity = Gravity.CENTER
						typeface = RupiUi.fontExtraBold(context)
						textSize = 15f
						setTextColor(Color.WHITE)
						background = RupiUi.rounded(
							if (index < 2) RupiUi.LEAF else Color.WHITE,
							dp(24).toFloat(), dp(3), if (index < 2) Color.WHITE else RupiUi.LEAF
						)
						contentDescription = if (index < 2) "Parada completada" else "Siguiente parada"
					}
					steps.addView(node, LinearLayout.LayoutParams(dp(42), dp(42)))
					if (index < 2) {
						steps.addView(View(context).apply { setBackgroundColor(RupiUi.LEAF) }, LinearLayout.LayoutParams(0, dp(5), 1f))
					}
				}
				goalCopy.addView(steps, LinearLayout.LayoutParams(-1, -2))
				button("VER META DE HOY", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) { onOpenDailyGoal() }
			}
		}
		addView(questsSection, LinearLayout.LayoutParams(-1, -2))

		// -----------------------------------------------------------------
		// TAB 3: LOGROS Y AULA (Trofeos y Tablero Escolar)
		// -----------------------------------------------------------------
		scaffold.into(achievementsSection) {
			fun compactAction(label: String, iconRes: Int, kind: RupiUi.Kind, action: () -> Unit) {
				val actionButton = Button(context).apply {
					text = label
					isAllCaps = false
					minHeight = dp(48)
					minWidth = dp(48)
					stateListAnimator = null
					typeface = RupiUi.fontExtraBold(context)
					textSize = 14f
					setTextColor(RupiUi.tactileText(kind))
					background = RupiUi.tactile(context, kind, radiusDp = 14)
					val icon = context.getDrawable(iconRes)?.mutate()?.apply { setTint(kind.text) }
					setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
					compoundDrawablePadding = dp(7)
					setPadding(dp(14), dp(8), dp(14), dp(12))
					setOnClickListener { action() }
				}
				scaffold.currentContainer().addView(actionButton, LinearLayout.LayoutParams(-2, dp(52)).apply {
					gravity = Gravity.CENTER_HORIZONTAL
					topMargin = dp(8)
					bottomMargin = dp(4)
				})
			}

			heroCard {
				chip("MISIÓN CUMPLIDA", RupiUi.LEAF_SOFT, RupiUi.SOFT_LINE, RupiUi.FOREST, iconRes = R.drawable.ic_star)
				val achievementIntro = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
				}
				val achievementCopy = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
				achievementCopy.addView(TextView(context).apply {
					text = "¡Tus logros escolares!"
					typeface = RupiUi.fontTitle(context)
					textSize = 21f
					setTextColor(RupiUi.FOREST)
				})
				achievementCopy.addView(TextView(context).apply {
					text = "Cada reto que completas te acerca a una nueva medalla."
					typeface = RupiUi.fontBody(context)
					textSize = 13.5f
					setTextColor(RupiUi.MUTED)
					setPadding(0, dp(4), 0, 0)
				})
				achievementIntro.addView(achievementCopy, LinearLayout.LayoutParams(0, -2, 1f))
				achievementIntro.addView(ImageView(context).apply {
					setImageResource(R.drawable.ic_rupi_mascot)
					scaleType = ImageView.ScaleType.FIT_CENTER
					contentDescription = "Rupi celebra tus logros"
				}, LinearLayout.LayoutParams(dp(66), dp(72)).apply { leftMargin = dp(6) })
				addView(achievementIntro, LinearLayout.LayoutParams(-1, -2))

				val medals = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER
					setPadding(0, dp(8), 0, dp(2))
				}
				val medalThemes = listOf(
					Triple(R.drawable.ic_star, Color.rgb(255, 224, 139), RupiUi.GOLD_STROKE),
					Triple(R.drawable.ic_award, RupiUi.LEAF_SOFT, RupiUi.LEAF_DARK),
					Triple(R.drawable.ic_book, Color.rgb(224, 238, 250), RupiUi.SKY_DARK)
				)
				medalThemes.forEachIndexed { index, medal ->
					val medalBadge = FrameLayout(context).apply {
						background = RupiUi.rounded(medal.second, dp(24).toFloat(), dp(2), RupiUi.LINE)
						contentDescription = listOf("Medalla de esfuerzo", "Medalla de constancia", "Medalla de aprendizaje")[index]
						addView(ImageView(context).apply {
							setImageResource(medal.first)
							drawable.mutate().setTint(medal.third)
							scaleType = ImageView.ScaleType.FIT_CENTER
							importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
						}, FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER))
					}
					medals.addView(medalBadge, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
						leftMargin = dp(7)
						rightMargin = dp(7)
					})
				}
				addView(medals, LinearLayout.LayoutParams(-1, -2))
				compactAction("Ver mis trofeos", R.drawable.ic_award, RupiUi.Kind.LOGIN_ORANGE) { onOpenAchievements() }
			}

			card {
				text("Unirme a mi aula", 19)
				muted("Aprende junto a tus compañeros. Pídele el código a tu profe.")
				val code = input("Código de aula", hintText = "Ej. AYACUCHO2026")
				code.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
				code.filters = arrayOf(InputFilter.LengthFilter(40), InputFilter.AllCaps())
				compactAction("Unirme al aula", R.drawable.ic_check, RupiUi.Kind.LEAF) {
					val rawCode = code.text.toString().trim()
					if (rawCode.isNotBlank()) onJoinByCode(rawCode)
					else scaffold.showLocalNotice("Escribe el código de tu aula para unirte.")
				}
			}
		}
		addView(achievementsSection, LinearLayout.LayoutParams(-1, -2))

		// -----------------------------------------------------------------
		// TAB 4: PERFIL DEL ESTUDIANTE (Mateo Quispe)
		// -----------------------------------------------------------------
		scaffold.into(profileSection) {
			// Cada ilustración conserva su proporción y cada acción tiene foco independiente.
			fun panel(image: Int, label: String, action: (() -> Unit)? = null) {
				val view = ImageView(context).apply {
					setImageResource(image)
					adjustViewBounds = true
					scaleType = ImageView.ScaleType.FIT_CENTER
					contentDescription = label
					if (action != null) {
						minimumHeight = dp(48)
						isFocusable = true
						val ripple = android.util.TypedValue()
						context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
						if (ripple.resourceId != 0) foreground = context.getDrawable(ripple.resourceId)
						setOnClickListener { action() }
					}
				}
				addView(view, LinearLayout.LayoutParams(-1, -2))
			}
			panel(R.drawable.profile_identity, "Mateo Quispe. Estudiante de tercero de primaria. I.E. 38001 Mariscal Sucre.")
			panel(R.drawable.profile_progress, "Mi progreso: 3 días de racha, 120 XP acumulados y 5 medallas.")
			panel(R.drawable.profile_account, "Mi cuenta")
			panel(R.drawable.profile_reminders, "Avisos y recordatorios de estudio", onOpenNotifications)
			panel(R.drawable.profile_downloads, "Resúmenes descargados para estudiar sin internet", onOpenSavedSummaries)
			panel(R.drawable.profile_logout, "Cerrar sesión escolar", onLogout)
		}
		addView(profileSection, LinearLayout.LayoutParams(-1, -2))
	}
	return routesContainer
}

fun renderTopicsRoutesList(
	scaffold: ScreenScaffold,
	routesContainer: LinearLayout,
	state: TopicsUiState,
	onOpenRoute: (LearningRouteSummary) -> Unit
) {
	lastTopicsStateCache = state
	lastOnOpenRouteCache = onOpenRoute

	routesContainer.removeAllViews()
	if (!state.routesLoaded) return
	val context = routesContainer.context
	val dp = { n: Int -> scaffold.dp(n) }

	scaffold.into(routesContainer) {
		val displayedRoutes = if (selectedRouteFilter == 0) {
			state.routes.filter { !it.title.contains("Comunidad", ignoreCase = true) }
		} else {
			state.routes.filter { it.title.contains("Comunidad", ignoreCase = true) }
		}

		if (displayedRoutes.isEmpty()) {
			if (selectedRouteFilter == 1) {
				card {
					text("Rutas de la Comunidad", 16)
					muted("Aún no tienes rutas comunitarias registradas. ¡Tu docente o tutor compartirá nuevos proyectos comunitarios muy pronto!")
				}
			} else {
				card { muted("Todavia no estas en ninguna ruta. Pidele a tu profe el codigo de clase para unirte.") }
			}
			return@into
		}

		// Agrupar con jerarquia visual: Rutas del grado de Mateo (3.° de primaria) vs Otras rutas
		val myGradeRoutes = displayedRoutes.filter { it.gradeName.contains("3", ignoreCase = true) }
		val otherRoutes = displayedRoutes.filterNot { it.gradeName.contains("3", ignoreCase = true) }

		fun renderRouteCard(route: LearningRouteSummary, isPriority: Boolean) {
			val cardLayout = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				background = if (isPriority) RupiUi.ayacuchoCard(context) else RupiUi.card(context)
				setPadding(dp(16), dp(16), dp(16), dp(16))
				isClickable = true
				isFocusable = true
				contentDescription = "${route.title}. ${route.areaName}. ${route.completedNodes} de ${route.totalNodes} paradas."
				setOnClickListener { onOpenRoute(route) }
			}

			// Fila superior: Chip de Area tematica + Chip de Grado
			val chipsRow = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
			}
			val (areaColor, areaBorder, areaText) = when {
				route.areaName.contains("Matem", ignoreCase = true) ->
					Triple(Color.rgb(238, 246, 251), Color.rgb(147, 196, 223), RupiUi.SKY_DARK)
				route.areaName.contains("Comun", ignoreCase = true) ->
					Triple(Color.rgb(255, 243, 240), Color.rgb(254, 189, 173), RupiUi.ORANGE_DARK)
				route.areaName.contains("Cien", ignoreCase = true) ->
					Triple(RupiUi.LEAF_SOFT, Color.rgb(182, 211, 168), RupiUi.LEAF_DARK)
				else ->
					Triple(Color.rgb(255, 250, 235), Color.rgb(255, 224, 138), RupiUi.SUN_DARK)
			}
			chipsRow.addView(TextView(context).apply {
				text = route.areaName.ifBlank { "Ruta Escolar" }
				typeface = RupiUi.fontExtraBold(context)
				textSize = 11.5f
				setTextColor(areaText)
				background = RupiUi.rounded(areaColor, dp(12).toFloat(), dp(1), areaBorder)
				setPadding(dp(10), dp(4), dp(10), dp(4))
			})
			if (route.gradeName.isNotBlank()) {
				chipsRow.addView(TextView(context).apply {
					text = route.gradeName
					typeface = RupiUi.fontBold(context)
					textSize = 11.5f
					setTextColor(RupiUi.MUTED)
					background = RupiUi.rounded(Color.WHITE, dp(12).toFloat(), dp(1), RupiUi.LINE)
					setPadding(dp(8), dp(4), dp(8), dp(4))
				}, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
			}
			cardLayout.addView(chipsRow, LinearLayout.LayoutParams(-1, -2))

			// Titulo grande y legible de la ruta
			cardLayout.addView(TextView(context).apply {
				text = route.title
				typeface = RupiUi.fontTitle(context)
				textSize = 18f
				setTextColor(RupiUi.FOREST)
				setPadding(0, dp(8), 0, dp(4))
				setLineSpacing(0f, 1.15f)
			})

			// Barra de progreso 3D
			val progressRow = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding(0, dp(4), 0, dp(4))
			}
			val pct = if (route.totalNodes > 0) ((route.completedNodes.toFloat() / route.totalNodes) * 100).toInt() else 0
			val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
				max = route.totalNodes.coerceAtLeast(1)
				progress = route.completedNodes
    progressTintList = ColorStateList.valueOf(if (route.isCompleted) RupiUi.LEAF else RupiUi.SUN_DARK)
				progressBackgroundTintList = ColorStateList.valueOf(RupiUi.PAPER_DEEP)
			}
			progressRow.addView(bar, LinearLayout.LayoutParams(0, dp(12), 1f))
			progressRow.addView(TextView(context).apply {
				text = "$pct%"
				typeface = RupiUi.fontExtraBold(context)
				textSize = 12f
    setTextColor(if (route.isCompleted) RupiUi.LEAF_DARK else RupiUi.SUN_DARK)
			}, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(10) })
			cardLayout.addView(progressRow, LinearLayout.LayoutParams(-1, -2))

			// Detalle de paradas
			cardLayout.addView(TextView(context).apply {
				text = "${route.completedNodes} de ${route.totalNodes} paradas completadas"
				typeface = RupiUi.fontBody(context)
				textSize = 12.5f
				setTextColor(RupiUi.MUTED)
			})

			// Boton de accion tactil 3D
			val actionBtn = Button(context).apply {
				text = if (route.isCompleted) "REPASAR RUTA" else "CONTINUAR RUTA"
				isAllCaps = false
				typeface = RupiUi.fontExtraBold(context)
				textSize = 13.5f
				minimumHeight = dp(46)
    background = RupiUi.tactile(context, if (route.isCompleted) RupiUi.Kind.LEAF else RupiUi.Kind.SUN, radiusDp = 12)
    setTextColor(if (route.isCompleted) Color.WHITE else RupiUi.FOREST)
				setOnClickListener { onOpenRoute(route) }
			}
			cardLayout.addView(actionBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

			addView(cardLayout, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
		}

		if (myGradeRoutes.isNotEmpty()) {
			section("Mis Rutas de Aula · 3.° de primaria")
			myGradeRoutes.forEach { route -> renderRouteCard(route, isPriority = true) }
		}

		if (otherRoutes.isNotEmpty()) {
			section(if (myGradeRoutes.isNotEmpty()) "Otras Rutas y Exploración" else "Rutas Escolares")
			otherRoutes.forEach { route -> renderRouteCard(route, isPriority = false) }
		}
	}
}
