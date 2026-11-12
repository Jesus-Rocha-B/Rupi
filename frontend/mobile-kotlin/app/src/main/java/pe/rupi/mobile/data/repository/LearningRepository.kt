package pe.rupi.mobile.data.repository

import pe.rupi.mobile.data.model.JoinRouteResult
import pe.rupi.mobile.data.model.LessonDetail
import pe.rupi.mobile.data.model.LearningRouteSummary
import pe.rupi.mobile.data.model.NodeCompletionResult
import pe.rupi.mobile.data.model.QuestionFeedback
import pe.rupi.mobile.data.model.RouteDetail
import pe.rupi.mobile.data.remote.RupiRemoteDataSource

/**
 * Repositorio para rutas de aprendizaje, código de clase, lecciones y evaluación de paradas.
 */
interface LearningRepository {
 suspend fun getLearningRoutes(): List<LearningRouteSummary>
 suspend fun joinRouteByCode(rawCode: String): JoinRouteResult
 suspend fun getRouteDetail(routeId: String): RouteDetail
 suspend fun startLesson(routeId: String, nodeId: String): LessonDetail
 suspend fun answerLessonQuestion(activityId: String, questionId: String, selectedOptionId: String): QuestionFeedback
 suspend fun completeNode(routeId: String, nodeId: String): NodeCompletionResult
}

class DefaultLearningRepository(
 private val remote: RupiRemoteDataSource
) : LearningRepository {

 override suspend fun getLearningRoutes(): List<LearningRouteSummary> =
  remote.getLearningRoutes()

 override suspend fun joinRouteByCode(rawCode: String): JoinRouteResult {
  val code = rawCode.trim().uppercase()
  require(code.isNotEmpty()) { "Escribe el código que te dio tu profe." }
  return remote.joinRouteByCode(code)
 }

 override suspend fun getRouteDetail(routeId: String): RouteDetail =
  remote.getRouteDetail(routeId)

 override suspend fun startLesson(routeId: String, nodeId: String): LessonDetail =
  remote.startLessonNode(routeId, nodeId)

 override suspend fun answerLessonQuestion(
  activityId: String,
  questionId: String,
  selectedOptionId: String
 ): QuestionFeedback = remote.answerQuestion(activityId, questionId, selectedOptionId)

 override suspend fun completeNode(routeId: String, nodeId: String): NodeCompletionResult =
  remote.completeNode(routeId, nodeId)
}
