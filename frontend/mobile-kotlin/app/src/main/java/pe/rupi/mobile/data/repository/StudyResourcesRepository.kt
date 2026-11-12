package pe.rupi.mobile.data.repository

import kotlinx.coroutines.CancellationException
import pe.rupi.mobile.data.local.LocalSummaryDataSource
import pe.rupi.mobile.data.local.SavedCopyCheck
import pe.rupi.mobile.data.local.SavedSummariesReadResult
import pe.rupi.mobile.data.model.AiPracticeStatus
import pe.rupi.mobile.data.model.FlashcardDeck
import pe.rupi.mobile.data.model.QuestionHint
import pe.rupi.mobile.data.model.RupiSessionExpired
import pe.rupi.mobile.data.model.StudyTopic
import pe.rupi.mobile.data.model.VisualSummaryLookup
import pe.rupi.mobile.data.remote.RupiRemoteDataSource

/**
 * Repositorio único para recursos formativos por tema: práctica IA, tarjetas de memoria
 * y resúmenes visuales (coordinando API REST remota con almacenamiento local sin conexión).
 */
interface StudyResourcesRepository {
 suspend fun getAiPractice(routeId: String, nodeId: String): AiPracticeStatus
 suspend fun requestAiPractice(routeId: String, nodeId: String): String
 suspend fun answerAiQuestion(practiceId: String, questionId: String, optionId: String)
 suspend fun reportAiQuestion(practiceId: String, questionId: String, reason: String)
 suspend fun getAiQuestionHint(practiceId: String, questionId: String): QuestionHint

 suspend fun getFlashcards(routeId: String, nodeId: String): FlashcardDeck
 suspend fun performFlashcardAction(routeId: String, nodeId: String, revision: Long, action: String, cardId: String)

 suspend fun loadVisualSummary(topic: StudyTopic): VisualSummaryLookup
 suspend fun generateVisualSummary(routeId: String, nodeId: String)
 suspend fun downloadAndSaveVisualSummary(topic: StudyTopic, resourceId: String)
 suspend fun reportVisualSummary(routeId: String, nodeId: String, resourceId: String, reason: String)
 suspend fun listSavedSummaries(): SavedSummariesReadResult
}

class DefaultStudyResourcesRepository(
 private val remote: RupiRemoteDataSource,
 private val localSummaries: LocalSummaryDataSource
) : StudyResourcesRepository {

 override suspend fun getAiPractice(routeId: String, nodeId: String): AiPracticeStatus =
  remote.getAiPractice(routeId, nodeId)

 override suspend fun requestAiPractice(routeId: String, nodeId: String): String =
  remote.generateAiPractice(routeId, nodeId)

 override suspend fun answerAiQuestion(practiceId: String, questionId: String, optionId: String) {
  remote.answerAiQuestion(practiceId, questionId, optionId)
 }

 override suspend fun reportAiQuestion(practiceId: String, questionId: String, reason: String) {
  remote.reportAiQuestion(practiceId, questionId, reason)
 }

 override suspend fun getAiQuestionHint(practiceId: String, questionId: String): QuestionHint =
  remote.getAiQuestionHint(practiceId, questionId)

 override suspend fun getFlashcards(routeId: String, nodeId: String): FlashcardDeck =
  remote.getFlashcards(routeId, nodeId)

 override suspend fun performFlashcardAction(
  routeId: String,
  nodeId: String,
  revision: Long,
  action: String,
  cardId: String
 ) {
  remote.postFlashcardAction(routeId, nodeId, revision, action, cardId)
 }

 override suspend fun loadVisualSummary(topic: StudyTopic): VisualSummaryLookup {
  return try {
   val online = remote.getVisualSummary(topic.routeId, topic.nodeId)
   val resource = online.resource
   if (resource != null) {
    val savedCheck = localSummaries.checkSavedCopy(topic.routeId, topic.nodeId)
    val outdated = when (savedCheck) {
     is SavedCopyCheck.Missing -> false
     is SavedCopyCheck.Invalid -> true
     is SavedCopyCheck.Valid -> savedCheck.version != resource.version
    }
    val retired = resource.state in RETIRED_SUMMARY_STATES
    if (outdated || retired) {
     localSummaries.deleteSaved(topic.routeId, topic.nodeId)
     online.copy(invalidatedPreviousCopy = true)
    } else {
     online
    }
   } else {
    localSummaries.deleteSaved(topic.routeId, topic.nodeId)
    online
   }
  } catch (e: CancellationException) {
   throw e
  } catch (e: RupiSessionExpired) {
   throw e
  } catch (e: Exception) {
   val saved = localSummaries.readSaved(topic.routeId, topic.nodeId) ?: throw e
   VisualSummaryLookup(
    resource = saved.resource,
    configured = false,
    offline = true
   )
  }
 }

 override suspend fun generateVisualSummary(routeId: String, nodeId: String) {
  remote.generateVisualSummary(routeId, nodeId)
 }

 override suspend fun downloadAndSaveVisualSummary(topic: StudyTopic, resourceId: String) {
  val zipBytes = remote.downloadVisualSummaryPackage(topic.routeId, topic.nodeId, resourceId)
  localSummaries.saveFromZipPackage(topic, zipBytes)
 }

 override suspend fun reportVisualSummary(routeId: String, nodeId: String, resourceId: String, reason: String) {
  remote.reportVisualSummary(routeId, nodeId, resourceId, reason)
 }

 override suspend fun listSavedSummaries(): SavedSummariesReadResult =
  localSummaries.listSaved()

 companion object {
  private val RETIRED_SUMMARY_STATES = setOf("REPORTADO", "RECHAZADO", "DESACTUALIZADO")
 }
}
