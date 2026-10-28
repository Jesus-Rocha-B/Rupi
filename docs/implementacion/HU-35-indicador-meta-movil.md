# HU-35 · Indicador gráfico de la meta diaria en Android

Implementación: 28 de octubre de 2026 · commit `cb4f88f`.

> Como alumno en una plataforma móvil, quiero ver un indicador gráfico (barra o círculo) que muestre mi avance hacia la meta del día.

## Criterios de aceptación (definidos en la implementación; el backlog no los trae)
- La app Android muestra un anillo con «completadas/meta» y el porcentaje, y una barra con el mismo avance.
- La meta y el avance son los mismos de la web (HU-34): un solo cálculo en el servidor.
- El alumno puede cambiar su meta desde el celular con las mismas opciones de la web (1, 2, 3 o 5 paradas).
- El indicador tiene descripción accesible y botones de 48 dp como mínimo.

## Uso
«Mis temas» → **Mi meta de hoy**: anillo, barra, frase de avance, mensaje motivador de Rupi, racha, semana de estudio (Hoy, LUN, MAR…) y selector de meta. **Actualizar mi avance** vuelve a consultar.

## API
Reutiliza HU-34: `GET /api/v1/student/daily-goal` y `PUT /api/v1/student/daily-goal/target?target=N`. No hay endpoint ni tabla nuevos.

## Código
- `DailyGoalRing.kt`: vista que dibuja el anillo (pista verde clara, arco verde hoja y dorado al lograrla) con `contentDescription` «Meta de hoy: X de Y lecciones completadas».
- `MainActivity.dailyGoal()`: pantalla y selector de meta.

## Nota
La pantalla «Mi ritmo de estudio y avisos» guarda otra meta (`gamificacion_preferencia_estudio.objetivo_actividades`, de 1 a 5) que solo usan los recordatorios de HU-25. El indicador usa `identidad_perfil_usuario.meta_diaria_actividades` (HU-34). Conviene unificarlas en una próxima revisión.

## Pruebas
- `assembleDebug` correcto.
- Emulador Pixel 8 (API 35) contra la API local: anillo 0/3, cambio de meta a 1 parada reflejado en el anillo y la frase.
