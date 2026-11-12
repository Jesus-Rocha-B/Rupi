package pe.rupi.mobile.data.model

import java.io.Serializable

/**
 * Resumen de una ruta de aprendizaje accesible para el estudiante.
 */
data class LearningRouteSummary(
 val versionRouteId: String,
 val title: String,
 val gradeName: String = "",
 val areaName: String = "",
 val completedNodes: Int = 0,
 val totalNodes: Int = 0
) : Serializable {
 fun subtitleText(): String =
  listOf(areaName, gradeName, "$completedNodes de $totalNodes paradas")
   .filter { it.isNotBlank() }
   .joinToString(" · ")

 val isCompleted: Boolean
  get() = totalNodes > 0 && completedNodes >= totalNodes
}

data class JoinRouteResult(
 val route: LearningRouteSummary,
 val message: String
)

enum class RouteNodeState(val label: String) {
 COMPLETADO("Completada"),
 EN_CURSO("En curso"),
 DISPONIBLE("Disponible"),
 BLOQUEADO("Bloqueada");

 companion object {
  fun fromApi(raw: String): RouteNodeState =
   entries.firstOrNull { it.name == raw } ?: BLOQUEADO
 }
}

enum class ActivityType(val label: String) {
 LECCION("Lección"),
 RETO("Reto"),
 CUESTIONARIO("Cuestionario");

 companion object {
  fun fromApi(raw: String): ActivityType = when (raw) {
   "RETO" -> RETO
   "CUESTIONARIO" -> CUESTIONARIO
   else -> LECCION
  }
 }
}

data class RouteNode(
 val id: String,
 val title: String,
 val sequence: Int,
 val state: RouteNodeState,
 val estimatedMinutes: Int = 0,
 val activityType: ActivityType = ActivityType.LECCION
) : Serializable

data class CurriculumUnitGroup(
 val title: String,
 val nodes: List<RouteNode>
)

data class RouteDetail(
 val route: LearningRouteSummary,
 val unitGroups: List<CurriculumUnitGroup>,
 val ungroupedNodes: List<RouteNode>
)

enum class LessonBlockType {
 TEXT,
 VIDEO,
 ADJUNTO,
 DESTACADO;

 companion object {
  fun fromApi(raw: String): LessonBlockType = when (raw) {
   "VIDEO" -> VIDEO
   "ADJUNTO" -> ADJUNTO
   "DESTACADO" -> DESTACADO
   else -> TEXT
  }
 }
}

data class LessonBlock(
 val type: LessonBlockType,
 val text: String?,
 val resourceUrl: String?,
 val accessibleText: String?
)

data class QuestionOption(
 val id: String,
 val text: String
)

data class LessonQuestion(
 val id: String,
 val statement: String,
 val explanation: String,
 val options: List<QuestionOption>
)

data class QuestionFeedback(
 val questionId: String,
 val isCorrect: Boolean,
 val correctOptionId: String,
 val explanation: String
)

data class LessonDetail(
 val activityId: String,
 val title: String,
 val instructions: String?,
 val state: String,
 val blocks: List<LessonBlock>,
 val questions: List<LessonQuestion>
)

data class NodeCompletionResult(
 val experienceEarned: Int,
 val rupiMessage: String,
 val badgeName: String?,
 val isRouteCompleted: Boolean,
 val routeBonusExperience: Int,
 val nextUnlockedNode: RouteNode?
)
