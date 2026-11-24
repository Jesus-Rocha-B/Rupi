package pe.rupi.mobile.ui.screens

import android.graphics.Color
import android.os.Build
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
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

			// 1. Mascota Rupi centrada y saludo limpio (Estilo Duolingo)
			val heroLayout = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				gravity = Gravity.CENTER_HORIZONTAL
				setPadding(0, dp(14), 0, dp(12))
			}
			heroLayout.addView(
				ImageView(context).apply {
					setImageResource(R.drawable.ic_rupi_mascot)
					scaleType = ImageView.ScaleType.FIT_CENTER
					importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
				},
				LinearLayout.LayoutParams(dp(88), dp(92))
			)

			val titleHeader = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER
				setPadding(0, dp(6), 0, dp(2))
			}
			titleHeader.addView(TextView(context).apply {
				text = if (isRegister) "Crea " else "Inicia sesión "
				typeface = RupiUi.fontTitle(context)
				textSize = 22f
				setTextColor(RupiUi.FOREST)
			})
			titleHeader.addView(TextView(context).apply {
				text = if (isRegister) "tu cuenta escolar" else "en tu cuenta"
				typeface = RupiUi.fontBody(context)
				textSize = 19f
				setTextColor(RupiUi.FOREST)
			})
			heroLayout.addView(titleHeader)

			heroLayout.addView(
				TextView(context).apply {
					text = if (isRegister) {
						"Regístrate para unirte a tu aula escolar y aprender jugando"
					} else {
						"Ingresa tus credenciales escolares para continuar tu aventura"
					}
					typeface = RupiUi.fontBody(context)
					textSize = 13.5f
					setTextColor(RupiUi.MUTED)
					gravity = Gravity.CENTER_HORIZONTAL
					setPadding(dp(16), dp(2), dp(16), 0)
				}
			)
			addView(heroLayout, LinearLayout.LayoutParams(-1, -2))

			var server: EditText? = null

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
						setPadding(dp(4), 0, 0, 0)
					})
					addView(termsBox)

					// Botón limpio sin iconos en el texto
					button("CREAR CUENTA", kind = RupiUi.Kind.PRIMARY) {
						val sText = server?.text?.toString()?.trim()?.ifBlank { defaultServer } ?: defaultServer
						val email = emailInput.text.toString().trim()
						val pass = regPasswordInput.text.toString()
						if (email.isBlank() || pass.isBlank()) {
							scaffold.showLocalNotice("Escribe tu correo y contraseña escolar para registrarte.")
						} else {
							onLogin(sText, email, pass)
						}
					}
				} else {
					// LOGIN / INICIO DE SESIÓN (Fiel a la imagen 2 de Duolingo)
					// Los campos llevan icono a la izquierda; los botones no llevan icono.
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
					})
					addView(forgotLayout, LinearLayout.LayoutParams(-1, -2))

					// Botón de Ingreso limpio (sin iconos en el botón)
					button("INGRESAR", kind = RupiUi.Kind.PRIMARY) {
						val serverText = server?.text?.toString()?.trim()?.ifBlank { defaultServer } ?: defaultServer
						val username = userInput.text.toString().trim()
						val secret = passwordInput.text.toString()
						passwordInput.setText("")
						onLogin(serverText, username, secret)
					}

					// Botón de acceso rápido con Mateo (sin iconos en el botón)
					button("PROBAR CON MATEO (DEMO)", kind = RupiUi.Kind.STICKER) {
						userInput.setText("estudiante.demo")
						passwordInput.setText("123456")
						val serverText = server?.text?.toString()?.trim()?.ifBlank { defaultServer } ?: defaultServer
						onLogin(serverText, "estudiante.demo", "123456")
					}
				}

				// Enlace para alternar entre Iniciar Sesión y Registrarse (Área táctil accesible de 48dp)
				val toggleRow = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER
					minimumHeight = dp(48)
					isClickable = true
					isFocusable = true
					setPadding(0, dp(6), 0, dp(4))
					setOnClickListener {
						renderAuthForm(isRegister = !isRegister)
					}
				}
				toggleRow.addView(TextView(context).apply {
					text = if (isRegister) "¿Ya tienes una cuenta? " else "¿No tienes una cuenta? "
					typeface = RupiUi.fontBody(context)
					textSize = 14f
					setTextColor(RupiUi.MUTED)
				})
				toggleRow.addView(TextView(context).apply {
					text = if (isRegister) "Inicia sesión" else "Regístrate"
					typeface = RupiUi.fontExtraBold(context)
					textSize = 14f
					setTextColor(RupiUi.LEAF_DARK)
				})
				addView(toggleRow, LinearLayout.LayoutParams(-1, -2))
			}

			// Botón de resúmenes sin conexión
			button("Estudiar sin conexión (Resúmenes descargados)", kind = RupiUi.Kind.QUIET) {
				onOpenSavedSummaries()
			}

			// Panel desplegable de ajustes de red y servidor escolar
			val serverContainer = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				visibility = View.GONE
				setPadding(dp(8), dp(4), dp(8), dp(6))
			}
			scaffold.into(serverContainer) {
				text("Ajustes de red y servidor escolar", 14)
				muted("Selecciona la dirección de conexión:")

				val presetsRow = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER
					setPadding(0, dp(4), 0, dp(6))
				}
				val pParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) }

				fun presetButton(label: String, url: String): Button {
					return Button(context).apply {
						text = label
						isAllCaps = false
						minHeight = dp(40)
						textSize = 11.5f
						typeface = RupiUi.fontExtraBold(context)
						background = RupiUi.tactile(context, RupiUi.Kind.QUIET, radiusDp = 10)
						setTextColor(RupiUi.FOREST)
						setPadding(dp(4), dp(4), dp(4), dp(4))
						setOnClickListener { server?.setText(url) }
					}
				}

				presetsRow.addView(presetButton("USB Celular\n(127.0.0.1)", "http://127.0.0.1:8081"), pParams)
				presetsRow.addView(presetButton("Emulador\n(10.0.2.2)", "http://10.0.2.2:8081"), pParams)
				presetsRow.addView(presetButton("Wi-Fi PC\n(10.200.174.1)", "http://10.200.174.1:8081"), pParams)
				presetsRow.addView(presetButton("Nube\n(api.rupi.pe)", "https://api.rupi.pe"), pParams)
				addView(presetsRow, LinearLayout.LayoutParams(-1, -2))

				server = input("URL del servidor", hintText = defaultServer).apply {
					setText(defaultServer)
				}
			}
			scaffold.addView(serverContainer, LinearLayout.LayoutParams(-1, -2))

			button("Ajustes de red", kind = RupiUi.Kind.QUIET) {
				val isVisible = serverContainer.visibility == View.VISIBLE
				serverContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
			}
		}
	}

	renderAuthForm(isRegister = false)
}

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
		// TAB 0: CAMINO DE APRENDIZAJE (Estilo Duolingo con Unit Banner)
		// -----------------------------------------------------------------
		scaffold.into(routesSection) {
			// Unit Banner estilo Duolingo (Sección y Unidad actual en verde vibrante)
			val unitBanner = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.tactile(context, RupiUi.Kind.LEAF, radiusDp = 18)
				setPadding(dp(18), dp(16), dp(16), dp(20))
			}
			val unitTexts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			unitTexts.addView(TextView(context).apply {
				text = "SECCION 1 · UNIDAD 1"
				typeface = RupiUi.fontExtraBold(context)
				textSize = 11.5f
				setTextColor(Color.rgb(219, 232, 210))
			})
			unitTexts.addView(TextView(context).apply {
				text = "Aventura de Palabras y Lectura"
				typeface = RupiUi.fontTitle(context)
				textSize = 19f
				setTextColor(Color.WHITE)
				setPadding(0, dp(2), 0, 0)
			})
			unitBanner.addView(unitTexts, LinearLayout.LayoutParams(0, -2, 1f))

			// Botón de cuaderno de apuntes en el banner
			val bookBtn = FrameLayout(context).apply {
				background = RupiUi.rounded(Color.WHITE, dp(14).toFloat(), dp(2), RupiUi.LEAF_DARK)
				setPadding(dp(8), dp(8), dp(8), dp(8))
				addView(ImageView(context).apply {
					setImageResource(R.drawable.ic_book)
					val d = drawable.mutate()
					d.setTint(RupiUi.LEAF_DARK)
					setImageDrawable(d)
				}, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
			}
			unitBanner.addView(bookBtn, LinearLayout.LayoutParams(dp(42), dp(42)))
			addView(unitBanner, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

			// Contenedor dinámico de rutas escolares
			routesContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			addView(routesContainer, LinearLayout.LayoutParams(-1, -2))

			button("Actualizar camino escolar", kind = RupiUi.Kind.QUIET, iconRes = R.drawable.ic_star) { onRefresh() }
		}
		addView(routesSection, LinearLayout.LayoutParams(-1, -2))

		// -----------------------------------------------------------------
		// TAB 1: TUTOR IA (CHAT CON RUPI - PERSONAJE PRINCIPAL)
		// -----------------------------------------------------------------
		scaffold.into(tutorSection) {
			val tutorHeader = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.tactile(context, RupiUi.Kind.STICKER, radiusDp = 16)
				setPadding(dp(14), dp(12), dp(14), dp(16))
			}
			tutorHeader.addView(
				ImageView(context).apply {
					setImageResource(R.drawable.ic_rupi_mascot)
					scaleType = ImageView.ScaleType.FIT_CENTER
					importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
				},
				LinearLayout.LayoutParams(dp(54), dp(58)).apply { rightMargin = dp(12) }
			)
			val tutorCopy = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			tutorCopy.addView(TextView(context).apply {
				text = "Tutor Rupi"
				typeface = RupiUi.fontTitle(context)
				textSize = 20f
				setTextColor(RupiUi.FOREST)
			})
			tutorCopy.addView(TextView(context).apply {
				text = "Tu companero de estudio socratico con IA"
				typeface = RupiUi.fontBody(context)
				textSize = 13f
				setTextColor(RupiUi.MUTED)
			})
			tutorHeader.addView(tutorCopy, LinearLayout.LayoutParams(0, -2, 1f))
			addView(tutorHeader, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

			val chatHistoryLayout = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(0, dp(4), 0, dp(8))
			}

			fun appendMessage(text: String, isUser: Boolean) {
				val bubbleFrame = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = if (isUser) Gravity.END else Gravity.START
					setPadding(0, dp(4), 0, dp(4))
				}
				val bubble = LinearLayout(context).apply {
					orientation = LinearLayout.VERTICAL
					background = if (isUser) {
						RupiUi.rounded(RupiUi.LEAF_SOFT, dp(14).toFloat(), dp(2), Color.rgb(182, 211, 168))
					} else {
						RupiUi.rounded(Color.WHITE, dp(14).toFloat(), dp(2), RupiUi.SOFT_LINE)
					}
					setPadding(dp(14), dp(10), dp(14), dp(10))
				}
				bubble.addView(TextView(context).apply {
					this.text = if (isUser) "Tu" else "Rupi"
					typeface = RupiUi.fontExtraBold(context)
					textSize = 12f
					setTextColor(if (isUser) RupiUi.LEAF_DARK else RupiUi.ORANGE_DARK)
				})
				bubble.addView(TextView(context).apply {
					this.text = text
					typeface = RupiUi.fontBody(context)
					textSize = 14.5f
					setTextColor(RupiUi.INK)
					setLineSpacing(0f, 1.2f)
					setPadding(0, dp(2), 0, 0)
				})
				val bubbleParams = LinearLayout.LayoutParams(-2, -2).apply {
					if (isUser) leftMargin = dp(40) else rightMargin = dp(40)
				}
				bubbleFrame.addView(bubble, bubbleParams)
				chatHistoryLayout.addView(bubbleFrame)
			}

			appendMessage("Hola, Mateo. Soy Rupi, tu tutor de primaria. Preguntame sobre cualquier ejercicio o tema escolar.", isUser = false)
			addView(chatHistoryLayout, LinearLayout.LayoutParams(-1, -2))

			lateinit var chatInput: EditText

			fun askQuestion(qText: String) {
				if (qText.isBlank()) return
				appendMessage(qText, isUser = true)
				val thinking = TextView(context).apply {
					text = "Rupi esta pensando..."
					typeface = RupiUi.fontBody(context)
					textSize = 13f
					setTextColor(RupiUi.MUTED)
					setPadding(dp(14), dp(4), dp(14), dp(4))
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

			val suggestions = listOf(
				"Como resuelvo fracciones de matematicas?",
				"Que es la fotosintesis en ciencias?",
				"Quienes fueron los Incas en el Peru?",
				"Dame un consejo para mi reto de hoy"
			)
			suggestions.forEach { prompt ->
				val btn = Button(context).apply {
					text = prompt
					isAllCaps = false
					minHeight = dp(40)
					typeface = RupiUi.fontBody(context)
					textSize = 13f
					background = RupiUi.rounded(Color.WHITE, dp(12).toFloat(), dp(1), RupiUi.LINE)
					setTextColor(RupiUi.FOREST)
					setPadding(dp(10), dp(6), dp(10), dp(6))
					setOnClickListener { askQuestion(prompt) }
				}
				addView(btn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
			}

			card {
				text("Preguntale a Rupi", 16)
				chatInput = input("Consulta escolar", hintText = "Escribe tu duda aqui...")
				button("ENVIAR CONSULTA", kind = RupiUi.Kind.PRIMARY, iconRes = R.drawable.ic_send) {
					val message = chatInput.text.toString().trim()
					if (message.isNotBlank()) {
						chatInput.setText("")
						askQuestion(message)
					}
				}
			}
		}
		addView(tutorSection, LinearLayout.LayoutParams(-1, -2))

		// -----------------------------------------------------------------
		// TAB 2: MISIONES Y RETOS (Estilo Quests de Duolingo)
		// -----------------------------------------------------------------
		scaffold.into(questsSection) {
			// Banner morado/vibrante estilo Duolingo Quests
			val questBanner = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.tactile(context, RupiUi.Kind.PRIMARY, radiusDp = 18)
				setPadding(dp(16), dp(16), dp(16), dp(20))
			}
			questBanner.addView(
				ImageView(context).apply {
					setImageResource(R.drawable.ic_chest)
					scaleType = ImageView.ScaleType.FIT_CENTER
				},
				LinearLayout.LayoutParams(dp(52), dp(52)).apply { rightMargin = dp(12) }
			)
			val qbTexts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			qbTexts.addView(TextView(context).apply {
				text = "Bienvenido a tus misiones!"
				typeface = RupiUi.fontTitle(context)
				textSize = 19f
				setTextColor(Color.WHITE)
			})
			qbTexts.addView(TextView(context).apply {
				text = "Completa tus misiones escolares para ganar puntos y cofres."
				typeface = RupiUi.fontBody(context)
				textSize = 13f
				setTextColor(Color.rgb(255, 240, 230))
			})
			questBanner.addView(qbTexts, LinearLayout.LayoutParams(0, -2, 1f))
			addView(questBanner, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

			// Tarjeta Misión diaria con barra de progreso
			card {
				val qRow = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
				}
				qRow.addView(ImageView(context).apply {
					setImageResource(R.drawable.ic_star)
					val d = drawable.mutate()
					d.setTint(RupiUi.SUN_DARK)
					setImageDrawable(d)
				}, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(8) })
				qRow.addView(TextView(context).apply {
					text = "Mision del dia: Gana 20 XP"
					typeface = RupiUi.fontExtraBold(context)
					textSize = 16f
					setTextColor(RupiUi.FOREST)
				}, LinearLayout.LayoutParams(0, -2, 1f))
				addView(qRow, LinearLayout.LayoutParams(-1, -2))

				val pBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
					max = 20
					progress = 14
					progressTintList = android.content.res.ColorStateList.valueOf(RupiUi.SUN_DARK)
					progressBackgroundTintList = android.content.res.ColorStateList.valueOf(RupiUi.PAPER_DEEP)
				}
				addView(pBar, LinearLayout.LayoutParams(-1, dp(14)).apply { topMargin = dp(8); bottomMargin = dp(4) })
				muted("14 de 20 XP completados hoy.")
			}

			// Reto del día y meta diaria
			card {
				text("Reto diario escolar", 17)
				muted("Acepta el reto para duplicar tus puntos:")
				button("COMENZAR RETO DEL DIA", kind = RupiUi.Kind.PRIMARY, iconRes = R.drawable.ic_star) { onOpenDailyChallenge() }
			}

			card {
				text("Mi meta diaria de paradas", 17)
				muted("Consulta tu ritmo de avance diario:")
				button("VER META DE HOY", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) { onOpenDailyGoal() }
			}
		}
		addView(questsSection, LinearLayout.LayoutParams(-1, -2))

		// -----------------------------------------------------------------
		// TAB 3: LOGROS Y AULA (Trofeos y Tablero Escolar)
		// -----------------------------------------------------------------
		scaffold.into(achievementsSection) {
			heroCard {
				chip("TABLERO Y TROFEOS", RupiUi.LEAF_SOFT, RupiUi.SOFT_LINE, RupiUi.FOREST, iconRes = R.drawable.ic_award)
				text("Tus Logros Escolares", 22)
				muted("Revisa tus medallas conseguidas y compite sanamente con tus companeros de aula.")
				button("ABRIR VITRINA DE TROFEOS", kind = RupiUi.Kind.SKY, iconRes = R.drawable.ic_award) { onOpenAchievements() }
			}

			card {
				text("Unirse a un aula con codigo", 18)
				muted("Si tu profesor te entrego un codigo escolar, escribelo aqui:")
				val code = input("Codigo de clase", hintText = "Ej: AYACUCHO2026")
				code.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
				code.filters = arrayOf(InputFilter.LengthFilter(40), InputFilter.AllCaps())
				button("UNIRME AL AULA", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) {
					val rawCode = code.text.toString().trim()
					if (rawCode.isNotBlank()) onJoinByCode(rawCode)
				}
			}
		}
		addView(achievementsSection, LinearLayout.LayoutParams(-1, -2))

		// -----------------------------------------------------------------
		// TAB 4: PERFIL DEL ESTUDIANTE (Mateo Quispe)
		// -----------------------------------------------------------------
		scaffold.into(profileSection) {
			stickerCard {
				val profHeader = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
				}
				profHeader.addView(
					ImageView(context).apply {
						setImageResource(R.drawable.ic_rupi_mascot)
						scaleType = ImageView.ScaleType.FIT_CENTER
						importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
					},
					LinearLayout.LayoutParams(dp(64), dp(68)).apply { rightMargin = dp(14) }
				)
				val profInfo = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
				profInfo.addView(TextView(context).apply {
					text = "Mateo Quispe"
					typeface = RupiUi.fontTitle(context)
					textSize = 24f
					setTextColor(RupiUi.FOREST)
				})
				profInfo.addView(TextView(context).apply {
					text = "Estudiante · 3.° de primaria"
					typeface = RupiUi.fontExtraBold(context)
					textSize = 14f
					setTextColor(RupiUi.LEAF)
				})
				profInfo.addView(TextView(context).apply {
					text = "I.E. 38001 Mariscal Sucre"
					typeface = RupiUi.fontBody(context)
					textSize = 13f
					setTextColor(RupiUi.MUTED)
				})
				profHeader.addView(profInfo, LinearLayout.LayoutParams(0, -2, 1f))
				addView(profHeader, LinearLayout.LayoutParams(-1, -2))
			}

			// Estadísticas estilo perfil Duolingo
			card {
				text("Estadisticas de Mateo", 18)
				val statsGrid = LinearLayout(context).apply {
					orientation = LinearLayout.VERTICAL
					setPadding(0, dp(4), 0, dp(4))
				}
				fun statRow(iconRes: Int, label: String, value: String, tint: Int) {
					val row = LinearLayout(context).apply {
						orientation = LinearLayout.HORIZONTAL
						gravity = Gravity.CENTER_VERTICAL
						setPadding(0, dp(6), 0, dp(6))
					}
					row.addView(ImageView(context).apply {
						setImageResource(iconRes)
						val d = drawable.mutate()
						d.setTint(tint)
						setImageDrawable(d)
					}, LinearLayout.LayoutParams(dp(20), dp(20)).apply { rightMargin = dp(8) })
					row.addView(TextView(context).apply {
						text = label
						typeface = RupiUi.fontBody(context)
						textSize = 14f
						setTextColor(RupiUi.MUTED)
					}, LinearLayout.LayoutParams(0, -2, 1f))
					row.addView(TextView(context).apply {
						text = value
						typeface = RupiUi.fontExtraBold(context)
						textSize = 14.5f
						setTextColor(RupiUi.FOREST)
					})
					statsGrid.addView(row)
				}
				statRow(R.drawable.ic_flame, "Racha de estudio:", "3 dias seguidos", RupiUi.ORANGE)
				statRow(R.drawable.ic_gem, "Puntos de experiencia:", "120 XP acumulados", Color.rgb(0, 180, 216))
				statRow(R.drawable.ic_award, "Trofeos escolares:", "5 medallas", RupiUi.SKY)
				addView(statsGrid, LinearLayout.LayoutParams(-1, -2))
			}

			section("Opciones de cuenta")
			button("Avisos y recordatorios de estudio", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_volume_2) { onOpenNotifications() }
			button("Resumenes descargados (sin internet)", kind = RupiUi.Kind.STICKER, iconRes = R.drawable.ic_book) { onOpenSavedSummaries() }
			button("Cerrar sesion escolar", kind = RupiUi.Kind.DANGER, iconRes = R.drawable.ic_arrow_left) { onLogout() }
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
	routesContainer.removeAllViews()
	if (!state.routesLoaded) return
	scaffold.into(routesContainer) {
		if (state.routes.isEmpty()) {
			card { muted("Todavia no estas en ninguna ruta. Pidele a tu profe el codigo de clase para unirte.") }
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
