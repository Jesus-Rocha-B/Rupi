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
 * Pantalla de inicio de sesion inspirada en la arquitectura limpia y accesible de Duolingo (sin emojis).
 */
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

		// Cabecera Duolingo: Mascota Rupi centrada con bienvenida limpia
		val heroLayout = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			gravity = Gravity.CENTER_HORIZONTAL
			setPadding(0, dp(12), 0, dp(16))
		}
		heroLayout.addView(
			ImageView(context).apply {
				setImageResource(R.drawable.ic_rupi_mascot)
				scaleType = ImageView.ScaleType.FIT_CENTER
				importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			},
			LinearLayout.LayoutParams(dp(90), dp(96))
		)
		heroLayout.addView(
			TextView(context).apply {
				text = "Rupi."
				typeface = RupiUi.fontTitle(context)
				textSize = 28f
				setTextColor(RupiUi.FOREST)
				gravity = Gravity.CENTER_HORIZONTAL
				setPadding(0, dp(4), 0, 0)
			}
		)
		heroLayout.addView(
			TextView(context).apply {
				text = "Tu aventura escolar para aprender jugando en primaria"
				typeface = RupiUi.fontBody(context)
				textSize = 14f
				setTextColor(RupiUi.MUTED)
				gravity = Gravity.CENTER_HORIZONTAL
				setPadding(dp(16), dp(2), dp(16), 0)
			}
		)
		addView(heroLayout, LinearLayout.LayoutParams(-1, -2))

		lateinit var user: EditText
		lateinit var password: EditText
		var server: EditText? = null

		stickerCard {
			text("Ingresa a tu cuenta", 20)
			muted("Escribe tus credenciales escolares o ingresa directo con la cuenta de prueba:")

			user = input("Usuario escolar", hintText = "Ej: estudiante.demo")
			password = input("Contraseña escolar", password = true, hintText = "Tu clave del colegio")

			button("INGRESAR A MI AULA", kind = RupiUi.Kind.PRIMARY, iconRes = R.drawable.ic_check) {
				val serverText = server?.text?.toString()?.trim()?.ifBlank { defaultServer } ?: defaultServer
				val username = user.text.toString().trim()
				val secret = password.text.toString()
				password.setText("")
				onLogin(serverText, username, secret)
			}

			button("PROBAR CON MATEO (DEMO)", kind = RupiUi.Kind.STICKER, iconRes = R.drawable.ic_star) {
				user.setText("estudiante.demo")
				password.setText("123456")
				val serverText = server?.text?.toString()?.trim()?.ifBlank { defaultServer } ?: defaultServer
				onLogin(serverText, "estudiante.demo", "123456")
			}

			muted("Olvidaste tu clave? Solicitala a tu profesor o profesora.")
		}

		button("Estudiar sin conexion (Resumenes descargados)", kind = RupiUi.Kind.QUIET, iconRes = R.drawable.ic_volume_2) {
			onOpenSavedSummaries()
		}

		// Panel de conexion de desarrollo
		val serverContainer = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
			setPadding(dp(8), dp(6), dp(8), dp(6))
		}
		scaffold.into(serverContainer) {
			text("Ajustes de servidor escolar", 15)
			muted("Selecciona la direccion de red de pruebas:")

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
					minHeight = dp(42)
					textSize = 12f
					typeface = RupiUi.fontExtraBold(context)
					background = RupiUi.tactile(context, RupiUi.Kind.QUIET, radiusDp = 10)
					setTextColor(RupiUi.FOREST)
					setPadding(dp(4), dp(4), dp(4), dp(6))
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

		button("Ajustes de conexion de red", kind = RupiUi.Kind.QUIET) {
			val isVisible = serverContainer.visibility == View.VISIBLE
			serverContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
		}
	}
}

/**
 * Pantalla principal con navegacion por pestanas estilo Duolingo:
 * Rutas (Camino) | Tutor IA (Chat) | Retos (Metas y Trofeos) | Perfil (Estudiante y Aula)
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

		// Barra superior de progreso del estudiante (estilo Duolingo: Racha y XP sin emojis)
		val topStatusBar = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			background = RupiUi.rounded(Color.WHITE, dp(14).toFloat(), dp(2), RupiUi.SOFT_LINE)
			setPadding(dp(12), dp(8), dp(12), dp(8))
		}
		val userLabel = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
		}
		userLabel.addView(TextView(context).apply {
			text = "Mateo Quispe"
			typeface = RupiUi.fontTitle(context)
			textSize = 15f
			setTextColor(RupiUi.FOREST)
		})
		userLabel.addView(TextView(context).apply {
			text = "3.° de primaria · I.E. 38001"
			typeface = RupiUi.fontBody(context)
			textSize = 12f
			setTextColor(RupiUi.MUTED)
		})
		topStatusBar.addView(userLabel, LinearLayout.LayoutParams(0, -2, 1f))

		fun statusBadge(iconRes: Int, textValue: String, colorTint: Int): LinearLayout {
			return LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.rounded(RupiUi.PAPER_DEEP, dp(10).toFloat(), dp(1), RupiUi.LINE)
				setPadding(dp(8), dp(4), dp(8), dp(4))
				addView(ImageView(context).apply {
					setImageResource(iconRes)
					val d = drawable.mutate()
					d.setTint(colorTint)
					setImageDrawable(d)
				}, LinearLayout.LayoutParams(dp(16), dp(16)))
				addView(TextView(context).apply {
					text = textValue
					typeface = RupiUi.fontExtraBold(context)
					textSize = 13f
					setTextColor(colorTint)
					setPadding(dp(4), 0, 0, 0)
				})
			}
		}

		topStatusBar.addView(statusBadge(R.drawable.ic_flame, "3 dias", RupiUi.ORANGE), LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
		topStatusBar.addView(statusBadge(R.drawable.ic_star, "120 XP", RupiUi.SUN_DARK), LinearLayout.LayoutParams(-2, -2))
		addView(topStatusBar, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

		// Barra de pestanas estilo Duolingo (4 secciones: Rutas, Tutor IA, Retos, Perfil)
		val tabsBar = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER
			setPadding(0, dp(4), 0, dp(8))
		}

		val routesSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.VISIBLE
		}
		val tutorSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}
		val challengesSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}
		val profileSection = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}

		fun selectTab(index: Int) {
			routesSection.visibility = if (index == 0) View.VISIBLE else View.GONE
			tutorSection.visibility = if (index == 1) View.VISIBLE else View.GONE
			challengesSection.visibility = if (index == 2) View.VISIBLE else View.GONE
			profileSection.visibility = if (index == 3) View.VISIBLE else View.GONE

			for (i in 0 until tabsBar.childCount) {
				val btn = tabsBar.getChildAt(i) as? Button ?: continue
				val selected = (i == index)
				btn.background = if (selected) {
					RupiUi.tactile(context, RupiUi.Kind.PRIMARY, radiusDp = 12)
				} else {
					RupiUi.tactile(context, RupiUi.Kind.QUIET, radiusDp = 12)
				}
				btn.setTextColor(if (selected) Color.WHITE else RupiUi.FOREST)
			}
		}

		fun createTab(title: String, index: Int, isInitial: Boolean, iconRes: Int): Button {
			return Button(context).apply {
				text = scaffold.iconLabel(iconRes, if (isInitial) Color.WHITE else RupiUi.FOREST, title, iconDp = 15)
				isAllCaps = false
				minHeight = dp(46)
				typeface = RupiUi.fontExtraBold(context)
				textSize = 12.5f
				stateListAnimator = null
				elevation = 0f
				setPadding(dp(6), dp(8), dp(6), dp(10))
				background = if (isInitial) {
					RupiUi.tactile(context, RupiUi.Kind.PRIMARY, radiusDp = 12)
				} else {
					RupiUi.tactile(context, RupiUi.Kind.QUIET, radiusDp = 12)
				}
				setTextColor(if (isInitial) Color.WHITE else RupiUi.FOREST)
				setOnClickListener { selectTab(index) }
			}
		}

		val tabParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
			setMargins(dp(2), 0, dp(2), 0)
		}
		tabsBar.addView(createTab("Rutas", 0, true, R.drawable.ic_play), tabParams)
		tabsBar.addView(createTab("Tutor IA", 1, false, R.drawable.ic_message_circle), tabParams)
		tabsBar.addView(createTab("Retos", 2, false, R.drawable.ic_flame), tabParams)
		tabsBar.addView(createTab("Perfil", 3, false, R.drawable.ic_user), tabParams)
		addView(tabsBar, LinearLayout.LayoutParams(-1, -2))

		// 1. SECCION: RUTAS DE APRENDIZAJE
		scaffold.into(routesSection) {
			mascotGuide(
				eyebrow = "Camino de Aprendizaje",
				message = "Elige una ruta escolar para avanzar parada por parada."
			)
			routesContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
			addView(routesContainer, LinearLayout.LayoutParams(-1, -2))
			button("Actualizar rutas escolares", kind = RupiUi.Kind.QUIET, iconRes = R.drawable.ic_star) { onRefresh() }
		}
		addView(routesSection, LinearLayout.LayoutParams(-1, -2))

		// 2. SECCION: TUTOR IA (CHAT CON RUPI - PERSONAJE PRINCIPAL)
		scaffold.into(tutorSection) {
			heroCard {
				val tutorHeader = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
				}
				tutorHeader.addView(
					ImageView(context).apply {
						setImageResource(R.drawable.ic_rupi_mascot)
						scaleType = ImageView.ScaleType.FIT_CENTER
						importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
					},
					LinearLayout.LayoutParams(dp(54), dp(58)).apply { rightMargin = dp(10) }
				)
				val tutorCopy = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
				tutorCopy.addView(TextView(context).apply {
					text = "Tutor Rupi"
					typeface = RupiUi.fontTitle(context)
					textSize = 20f
					setTextColor(RupiUi.FOREST)
				})
				tutorCopy.addView(TextView(context).apply {
					text = "Tu guia de aprendizaje socratico con IA"
					typeface = RupiUi.fontBody(context)
					textSize = 13f
					setTextColor(RupiUi.MUTED)
				})
				tutorHeader.addView(tutorCopy, LinearLayout.LayoutParams(0, -2, 1f))
				addView(tutorHeader, LinearLayout.LayoutParams(-1, -2))
			}

			// Historial de mensajes de chat interactivo
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

			// Mensaje inicial de Rupi
			appendMessage("Hola, Mateo. Soy Rupi, tu tutor de primaria. Preguntame cualquier duda sobre tus tareas o lecciones de hoy.", isUser = false)
			addView(chatHistoryLayout, LinearLayout.LayoutParams(-1, -2))

			// Preguntas rapidas sugeridas
			val suggestionsTitle = TextView(context).apply {
				text = "Preguntas sugeridas para practicar:"
				typeface = RupiUi.fontExtraBold(context)
				textSize = 13f
				setTextColor(RupiUi.MUTED)
				setPadding(dp(2), dp(4), 0, dp(2))
			}
			addView(suggestionsTitle, LinearLayout.LayoutParams(-1, -2))

			fun promptChip(label: String): Button {
				return Button(context).apply {
					text = label
					isAllCaps = false
					minHeight = dp(40)
					typeface = RupiUi.fontBody(context)
					textSize = 13f
					background = RupiUi.rounded(Color.WHITE, dp(12).toFloat(), dp(1), RupiUi.LINE)
					setTextColor(RupiUi.FOREST)
					setPadding(dp(10), dp(6), dp(10), dp(6))
				}
			}

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
				val chipBtn = promptChip(prompt).apply {
					setOnClickListener {
						askQuestion(prompt)
					}
				}
				addView(chipBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
			}

			card {
				text("Escribe tu pregunta a Rupi", 16)
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

		// 3. SECCION: RETOS & METAS DIARIAS
		scaffold.into(challengesSection) {
			heroCard {
				chip("ACTIVIDAD DIARIA", RupiUi.LEAF_SOFT, RupiUi.SOFT_LINE, RupiUi.FOREST, iconRes = R.drawable.ic_flame)
				text("Retos y Metas de Hoy", 22)
				muted("Un reto nuevo cada dia para ganar puntos de experiencia y medallas escolares.")
				button("Reto del dia", kind = RupiUi.Kind.PRIMARY, iconRes = R.drawable.ic_star) { onOpenDailyChallenge() }
				button("Mi meta de paradas", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) { onOpenDailyGoal() }
				button("Mis logros y trofeos", kind = RupiUi.Kind.SKY, iconRes = R.drawable.ic_award) { onOpenAchievements() }
			}
		}
		addView(challengesSection, LinearLayout.LayoutParams(-1, -2))

		// 4. SECCION: PERFIL DEL ESTUDIANTE Y AULA ESCOLAR
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
					LinearLayout.LayoutParams(dp(60), dp(64)).apply { rightMargin = dp(12) }
				)
				val profInfo = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
				profInfo.addView(TextView(context).apply {
					text = "Mateo Quispe"
					typeface = RupiUi.fontTitle(context)
					textSize = 22f
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

				chip("Estudiante Activo", RupiUi.LEAF_SOFT, Color.rgb(182, 211, 168), RupiUi.LEAF_DARK, iconRes = R.drawable.ic_check)
			}

			// Estadisticas escolares
			card {
				text("Estadisticas de aprendizaje", 18)
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
				statRow(R.drawable.ic_star, "Puntos de experiencia:", "120 XP", RupiUi.SUN_DARK)
				statRow(R.drawable.ic_award, "Trofeos y medallas:", "5 logros escolares", RupiUi.SKY)
				addView(statsGrid, LinearLayout.LayoutParams(-1, -2))
			}

			// Unirse a aula por codigo
			card {
				text("Unirse a una clase con codigo", 18)
				muted("Si tu docente te compartio un codigo de aula, ingresalo aqui:")
				val code = input("Codigo de clase", hintText = "Ej: AYACUCHO2026")
				code.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
				code.filters = arrayOf(InputFilter.LengthFilter(40), InputFilter.AllCaps())
				button("Unirme a la ruta de clase", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) {
					val rawCode = code.text.toString().trim()
					if (rawCode.isNotBlank()) onJoinByCode(rawCode)
				}
			}

			section("Herramientas escolares")
			button("Avisos y recordatorios de estudio", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_star) { onOpenNotifications() }
			button("Resumenes descargados (sin internet)", kind = RupiUi.Kind.STICKER, iconRes = R.drawable.ic_volume_2) { onOpenSavedSummaries() }
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
