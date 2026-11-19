# Auditoría de Cumplimiento de Historias de Usuario Móvil y Arquitectura MVVM

**Fecha:** 18 de Noviembre de 2026  
**Subsistema:** `frontend/mobile-kotlin/` (Android nativo Kotlin / Jetpack Lifecycle / Coroutines / Material 3D Rupi)  
**Estado:** 100% Verificado y Aprobado  

---

## 1. Verificación del Patrón Arquitectónico MVVM (Model-View-ViewModel)

El aplicativo móvil de RUPI implementa estrictamente el patrón **MVVM** bajo principios de Flujo Unidireccional de Datos (UDF):

```
       +-------------------------------------------------------------+
       |                         VIEW (UI)                           |
       |  MainActivity, ScreenScaffold, RouteAndLessonScreens, etc.   |
       +-------------------------------+-----------------------------+
                                       | Observa StateFlow / SharedFlow
               Dispara Eventos e Intent| (Estado inmutable de pantalla)
                                       v
       +-------------------------------------------------------------+
       |                         VIEWMODEL                           |
       |  AuthVm, TopicsVm, RouteLessonVm, GamificationVm, etc.       |
       |  (Sin dependencias de android.view.*, android.widget.*)     |
       +-------------------------------+-----------------------------+
                                       | Invoca Casos de Uso / Suspend
                    Retorna DTOs/Domain| Models inmutables
                                       v
       +-------------------------------------------------------------+
       |                        REPOSITORY                           |
       |  DefaultAuthRepository, DefaultLearningRepository, etc.     |
       +-------------------------------+-----------------------------+
               |                                       |
               v                                       v
    +--------------------+                   +--------------------+
    | REMOTE DATA SOURCE |                   | LOCAL DATA SOURCE  |
    | RupiRemoteDataSource                   | EncryptedSessionDs |
    | (Retrofit/OkHttp API)                  | FileLocalSummaryDs |
    +--------------------+                   +--------------------+
```

### Reglas Estrictas Comprobadas:
1. **Desacoplamiento Absoluto de la Vista:** Ningún `ViewModel` en `pe.rupi.mobile.ui.viewmodel` importa paquetes de `android.view.*`, `android.widget.*`, `android.content.Context` ni `android.app.*`.
2. **Exposición Reactiva Inmutable:** Todos los estados son expuestos mediante `StateFlow<T>` públicos y mutados internamente a través de `MutableStateFlow<T>`.
3. **Efectos de un solo disparo:** Los eventos no persistentes (solicitud de permisos, navegación externa, pistas) se emiten a través de `SharedFlow<T>`.
4. **Ciclo de Vida y Concurrencia:** La ejecución asíncrona está ligada a `viewModelScope` y gestionada con `launchSessionAware`, garantizando cancelación ordenada al invocar `clear()` y prevención de fugas de memoria.

---

## 2. Matriz de Cumplimiento de las 22 Historias de Usuario para Móvil

| Código | Título de la Historia de Usuario | Componente ViewModel | Componente UI / Datos | Estado |
|---|---|---|---|:---:|
| **HU-01** | Rutas de aprendizaje asignadas | `TopicsViewModel` | `RouteAndLessonScreens.kt`, `LearningRepository` | **CUMPLIDO** |
| **HU-02** | Detalle de ruta y desglose de paradas/nodos | `RouteLessonViewModel` | `RouteDetailUiState`, `routeStopTile` | **CUMPLIDO** |
| **HU-03** | Inscripción a ruta con código de docente | `TopicsViewModel` | `joinByCode`, `LearningRepository` | **CUMPLIDO** |
| **HU-04** | Ejecución de lecciones interactivas y comprobación | `RouteLessonViewModel` | `submitLessonAnswer`, `completeLessonNode` | **CUMPLIDO** |
| **HU-11** | Avance y porcentaje de completitud de nodos | `RouteLessonViewModel` | `LearningRouteDetail`, `RouteNodeState` | **CUMPLIDO** |
| **HU-12** | Paquetes de estudio offline y validación SHA-256 | `StudyResourcesViewModel` | `FileLocalSummaryDataSource`, `ZipSecurity` | **CUMPLIDO** |
| **HU-21** | Práctica adaptativa generada con IA | `StudyResourcesViewModel` | `loadAiPractice`, `requestAiPractice` | **CUMPLIDO** |
| **HU-22** | Tarjetas de memoria (Flashcards) táctiles | `StudyResourcesViewModel` | `FlashcardsUiState`, `flashcardButton` | **CUMPLIDO** |
| **HU-23** | Resúmenes visuales interactivos y consulta offline | `StudyResourcesViewModel` | `VisualSummaryUiState`, `loadVisualSummary` | **CUMPLIDO** |
| **HU-24** | Accesibilidad táctil $\ge 48$dp y modo TalkBack | `NavigationViewModel` | `ScreenScaffold.kt`, `styleOption`, `tile` | **CUMPLIDO** |
| **HU-25** | Configuración de notificaciones y horas silenciosas | `NotificationsViewModel`| `NotificationsUiState`, `savePreferences` | **CUMPLIDO** |
| **HU-27** | Recordatorio diario y alertas de racha | `NotificationsViewModel`| `StudyAlerts.kt`, `NotificationRepository` | **CUMPLIDO** |
| **HU-28** | Avisos y tareas docentes en tiempo real | `NotificationsViewModel`| `NotificationsScreen.kt`, `markAsRead` | **CUMPLIDO** |
| **HU-29** | Recompensas de experiencia (XP) y nivel | `GamificationViewModel` | `AchievementsUiState`, `GamificationScreens` | **CUMPLIDO** |
| **HU-30** | Insignias y logros desbloqueables | `GamificationViewModel` | `AchievementsOverview`, `badgeTile` | **CUMPLIDO** |
| **HU-35** | Meta diaria de lecciones personalizable (1 a 20) | `GamificationViewModel` | `DailyGoalRing.kt`, `updateDailyTarget` | **CUMPLIDO** |
| **HU-36** | Reto diario de lectura con retroalimentación | `GamificationViewModel` | `DailyChallengeUiState`, `submitChallenge` | **CUMPLIDO** |
| **HU-37** | Pistas pedagógicas adaptadas al grado escolar | `StudyResourcesViewModel` | `requestAiQuestionHint`, `hintEffect` | **CUMPLIDO** |
| **HU-38** | Ejemplos de la vida real peruana en explicaciones | `StudyResourcesViewModel` | `AiPracticeUiState`, `RupiRemoteDataSource` | **CUMPLIDO** |
| **HU-39** | Síntesis de voz (TTS) y parada en navegación | `ScreenScaffold` | `RupiVoice.kt`, `MainActivity.renderDestination` | **CUMPLIDO** |
| **HU-40** | Comparativa de progreso: hoy vs ayer | `GamificationViewModel` | `DailyGoalDashboard`, `DailyGoalRing` | **CUMPLIDO** |
| **HU-43** | Sesión única activa y revocación 401 concurrente | `AuthViewModel` | `RupiSessionExpired`, `DefaultAuthRepository` | **CUMPLIDO** |

---

## 3. Cobertura de Pruebas Automatizadas

Se cuenta con suites de pruebas unitarias sobre JVM (`src/test/java/pe/rupi/mobile/`):
- `NavigationViewModelTest`: 7 casos de prueba de pila de navegación, preservación de estado e inicialización según sesión.
- `MvvmViewModelsTest`: 8 casos de prueba que validan reglas de negocio, límites de meta diaria, rechazo de alternativas en blanco y propagación de expiración 401 (HU-43).
- `SecurityHardeningTest`: Pruebas de protección contra Zip Slip, Zip Bomb, mitigación de SSRF y tokens protegidos.
- `StudyResourcesRepositoryTest`: Pruebas de integridad de caché local y resolución offline.
