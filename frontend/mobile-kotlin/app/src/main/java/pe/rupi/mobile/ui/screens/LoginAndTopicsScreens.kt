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

	fun switchTab(index: Int) {
		routesSection.visibility = if (index == 0) View.VISIBLE else View.GONE
		tutorSection.visibility = if (index == 1) View.VISIBLE else View.GONE
		questsSection.visibility = if (index == 2) View.VISIBLE else View.GONE
		achievementsSection.visibility = if (index == 3) View.VISIBLE else View.GONE
		profileSection.visibility = if (index == 4) View.VISIBLE else View.GONE
	}

	scaffold.mountDuolingoShell(
		streakDays = 3,
		xpPoints = 120,
		hearts = 5,
		activeTab = 0,
		hideTopBarTabs = setOf(2, 3, 4),
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
					Color.rgb(255, 246, 227),
					dp(20).toFloat(),
					dp(1),
					RupiUi.LINE
				)
				setPadding(dp(14), dp(16), dp(14), dp(14))
			}

			// CABECERA DEL CHAT (Avatar + Nombre + Estado En Línea)
			val chatHeader = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.rounded(Color.WHITE, dp(16).toFloat())
                setPadding(dp(10), dp(8), dp(10), dp(8))
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
				setTextColor(RupiUi.FOREST)
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
				setTextColor(RupiUi.LEAF)
			})
			headerTexts.addView(statusRow)
			chatHeader.addView(headerTexts, LinearLayout.LayoutParams(0, -2, 1f))
			chatBox.addView(chatHeader)

			chatBox.addView(View(context).apply {
				setBackgroundColor(Color.TRANSPARENT)
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
				text = "Preguntas rápidas:"
				typeface = RupiUi.fontExtraBold(context)
				textSize = 12f
				setTextColor(RupiUi.FOREST)
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
						RupiUi.rounded(Color.rgb(228, 248, 203), dp(16).toFloat(), 0, 0)
					} else {
						RupiUi.rounded(Color.rgb(255, 253, 246), dp(16).toFloat(), dp(1), RupiUi.LINE)
					}
					setPadding(dp(14), dp(10), dp(14), dp(10))
				}

				bubble.addView(TextView(context).apply {
					this.text = text
					typeface = RupiUi.fontBody(context)
					textSize = 14f
					setTextColor(RupiUi.FOREST)
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

			appendMessage("¡Hola, Mateo! ¿Qué practicamos hoy?", isUser = false)
			val conversation = FrameLayout(context)
            conversation.addView(ImageView(context).apply {
                setImageResource(R.drawable.tutor_landscape)
                scaleType = ImageView.ScaleType.FIT_XY
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(-1, dp(155), Gravity.BOTTOM))
            val historyScroll = android.widget.ScrollView(context).apply { addView(chatHistoryLayout) }
            conversation.addView(historyScroll)
            chatHistoryLayout.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom > oldBottom) historyScroll.post { historyScroll.smoothScrollTo(0, chatHistoryLayout.height) }
            }
            chatBox.addView(conversation, LinearLayout.LayoutParams(-1, dp((context.resources.configuration.screenHeightDp - 365).coerceAtLeast(220))))

			askQuestion = { qText: String ->
				if (qText.isNotBlank()) {
					
					appendMessage(qText, isUser = true)

					val thinking = TextView(context).apply {
						text = "Rupi está escribiendo..."
						typeface = RupiUi.fontBody(context)
						textSize = 12.5f
						setTextColor(RupiUi.FOREST)
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
			val labels = listOf("½  Fracciones", "☘  Fotosíntesis", "⌂  Los incas", "◎  Mi meta de hoy")
            suggestions.chunked(2).forEachIndexed { rowIndex, prompts ->
                val row = LinearLayout(context)
                suggestionsContainer.addView(row)
                prompts.forEachIndexed { column, prompt ->
				val btn = Button(context).apply {
					text = labels[rowIndex * 2 + column]
					isAllCaps = false
					minHeight = dp(48)
					typeface = RupiUi.fontBody(context)
					textSize = 12.5f
					background = RupiUi.rounded(Color.rgb(242, 249, 223), dp(12).toFloat(), dp(1), RupiUi.LEAF)
					setTextColor(RupiUi.FOREST)
					setPadding(dp(12), dp(6), dp(12), dp(6))
					setOnClickListener { askQuestion(prompt) }
				}
				row.addView(btn, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
                }
			}
			chatBox.addView(suggestionsContainer)

			val inputBar = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.rounded(Color.WHITE, dp(24).toFloat(), dp(1), RupiUi.LINE)
				setPadding(dp(14), dp(4), dp(6), dp(4))
			}
			chatInput = EditText(context).apply {
				hint = "Escribe tu duda escolar aquí..."
				setHintTextColor(RupiUi.MUTED)
				setTextColor(RupiUi.FOREST)
				textSize = 13.5f
				typeface = RupiUi.fontBody(context)
				background = null
				isSingleLine = true
			}
			inputBar.addView(chatInput, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })

			val sendBtn = ImageView(context).apply {
				contentDescription = "Enviar pregunta"
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
			fun missionPanel(art: Int, description: String, action: (() -> Unit)? = null) {
				val panel = ImageView(context).apply {
					setImageResource(art)
					adjustViewBounds = true
					scaleType = ImageView.ScaleType.FIT_CENTER
					contentDescription = description
					if (action != null) {
						isFocusable = true
						setOnClickListener { action() }
						val ripple = android.util.TypedValue()
						context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
						if (ripple.resourceId != 0) foreground = context.getDrawable(ripple.resourceId)
					}
				}
				addView(panel, LinearLayout.LayoutParams(-1, -2))
			}
			missionPanel(R.drawable.mission_welcome, "Misiones de hoy. Completa retos y gana puntos para tu aventura.")
			missionPanel(R.drawable.mission_progress, "Misión del día: gana 20 XP. 14 de 20 XP completados hoy.")
			missionPanel(R.drawable.mission_challenge, "Comenzar reto diario escolar para duplicar tus puntos", onOpenDailyChallenge)
			missionPanel(R.drawable.mission_goal, "Ver meta de hoy. Dos de tres paradas completadas.", onOpenDailyGoal)
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
				chip("TABLERO Y TROFEOS", RupiUi.LEAF_SOFT, RupiUi.SOFT_LINE, RupiUi.FOREST, iconRes = R.drawable.ic_award)
				text("Tus logros escolares", 24)
				muted("Descubre tus medallas y celebra lo que aprendes.")
				addView(ImageView(context).apply {
					setImageResource(R.drawable.achievements_medals)
					adjustViewBounds = true
					scaleType = ImageView.ScaleType.FIT_CENTER
					contentDescription = "Rupi presenta tres medallas: copa, estrella y libro."
				}, LinearLayout.LayoutParams(-1, -2))
				compactAction("Ver mis trofeos", R.drawable.ic_award, RupiUi.Kind.LOGIN_ORANGE) { onOpenAchievements() }
			}

			card {
				text("Unirme a mi aula", 23)
				muted("Escribe el código que te dio tu profe para aprender con tu clase.")
				val code = input("Código de aula", hintText = "Ej. AYACUCHO2026")
				code.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
				code.filters = arrayOf(InputFilter.LengthFilter(40), InputFilter.AllCaps())
				compactAction("Unirme al aula", R.drawable.ic_check, RupiUi.Kind.LEAF) {
					val rawCode = code.text.toString().trim()
					if (rawCode.isNotBlank()) onJoinByCode(rawCode)
					else scaffold.showLocalNotice("Escribe el código de tu aula para unirte.")
				}
				chip("ⓘ  Pídele el código a tu profesor o profesora.", RupiUi.LEAF_SOFT, RupiUi.SOFT_LINE, RupiUi.FOREST)
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
