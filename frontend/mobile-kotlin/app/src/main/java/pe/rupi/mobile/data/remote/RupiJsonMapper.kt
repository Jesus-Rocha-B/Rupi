package pe.rupi.mobile.data.remote

import org.json.JSONArray
import org.json.JSONObject
import pe.rupi.mobile.data.model.*

/**
 * Transforma respuestas JSON del backend Spring Boot en modelos inmutables del dominio móvil.
 */
object RupiJsonMapper {

 private fun optNonNullString(json: JSONObject, key: String): String? =
  json.optString(key).takeIf { it.isNotBlank() && it != "null" }

 fun parseLearningRoutes(json: JSONObject): List<LearningRouteSummary> {
  val items = json.getJSONArray("items")
  return List(items.length()) { index ->
   val item = items.getJSONObject(index)
   LearningRouteSummary(
    versionRouteId = item.getString("versionRouteId"),
    title = item.getString("title"),
    gradeName = item.optJSONObject("grade")?.optString("name").orEmpty(),
    areaName = item.optJSONObject("area")?.optString("name").orEmpty(),
    completedNodes = item.optInt("completedNodes"),
    totalNodes = item.optInt("totalNodes")
   )
  }
 }

 fun parseJoinRouteResult(json: JSONObject): JoinRouteResult {
  val title = json.optString("routeTitle")
  val route = LearningRouteSummary(
   versionRouteId = json.getString("versionRouteId"),
   title = title,
   gradeName = json.optJSONObject("grade")?.optString("name").orEmpty(),
   areaName = json.optJSONObject("area")?.optString("name").orEmpty()
  )
  val message = json.optString("message").ifBlank { "¡Listo! Ya estás en «$title»." }
  return JoinRouteResult(route = route, message = message)
 }

 fun parseRouteDetail(json: JSONObject): RouteDetail {
  val progress = json.getJSONObject("progress")
  val route = LearningRouteSummary(
   versionRouteId = json.getString("versionRouteId").ifBlank { "" },
   title = json.getString("title"),
   gradeName = json.optJSONObject("grade")?.optString("name").orEmpty(),
   areaName = json.optJSONObject("area")?.optString("name").orEmpty(),
   completedNodes = progress.optInt("completedNodes"),
   totalNodes = progress.optInt("totalNodes")
  )
  val nodesArray = json.getJSONArray("nodes")
  val nodes = List(nodesArray.length()) { index ->
   val n = nodesArray.getJSONObject(index)
   RouteNode(
    id = n.getString("id"),
    title = n.getString("title"),
    sequence = n.optInt("sequence"),
    state = RouteNodeState.fromApi(n.optString("state")),
    estimatedMinutes = n.optInt("estimatedMinutes"),
    activityType = ActivityType.fromApi(n.optString("activityType"))
   )
  }
  val byId = nodes.associateBy { it.id }
  val shown = mutableSetOf<String>()
  val unitGroups = mutableListOf<CurriculumUnitGroup>()
  val units = json.optJSONObject("curriculum")?.optJSONArray("units")
  if (units != null) {
   for (u in 0 until units.length()) {
    val unit = units.getJSONObject(u)
    val ids = unit.optJSONArray("nodeIds")?.let { arr -> List(arr.length()) { arr.getString(it) } }
     ?: unit.optJSONArray("nodes")?.let { arr -> List(arr.length()) { arr.getJSONObject(it).getString("id") } }
     ?: emptyList()
    val inUnit = ids.mapNotNull { byId[it] }.filter { shown.add(it.id) }
    if (inUnit.isNotEmpty()) {
     unitGroups.add(CurriculumUnitGroup(title = unit.optString("title").ifBlank { "Unidad ${u + 1}" }, nodes = inUnit))
    }
   }
  }
  val ungrouped = nodes.filter { it.id !in shown }
  return RouteDetail(route = route, unitGroups = unitGroups, ungroupedNodes = ungrouped)
 }

 fun parseLessonDetail(json: JSONObject, resolveUrl: (String) -> String?): LessonDetail {
  val blocksJson = json.optJSONArray("blocks") ?: JSONArray()
  val blocks = List(blocksJson.length()) { index ->
   val block = blocksJson.getJSONObject(index)
   val rawType = block.optString("type")
   val parsed = AttachmentParser.parse(
    type = rawType,
    rawBody = optNonNullString(block, "text"),
    rawUrl = optNonNullString(block, "url")
   )
   LessonBlock(
    type = LessonBlockType.fromApi(rawType),
    text = parsed.displayText,
    resourceUrl = parsed.rawUrl?.let(resolveUrl),
    accessibleText = optNonNullString(block, "accessibleText")
   )
  }
  val questionsJson = json.optJSONArray("questions") ?: JSONArray()
  return LessonDetail(
   activityId = json.optString("activityId"),
   title = json.optString("title"),
   instructions = optNonNullString(json, "instructions"),
   state = json.optString("state"),
   blocks = blocks,
   questions = parseLessonQuestions(questionsJson)
  )
 }

 private fun parseLessonQuestions(questionsJson: JSONArray): List<LessonQuestion> =
  List(questionsJson.length()) { i ->
   val q = questionsJson.getJSONObject(i)
   val optsJson = q.getJSONArray("options")
   LessonQuestion(
    id = q.getString("id"),
    statement = q.getString("statement"),
    explanation = q.optString("explanation"),
    options = List(optsJson.length()) { j ->
     val opt = optsJson.getJSONObject(j)
     QuestionOption(id = opt.getString("id"), text = opt.getString("text"))
    }
   )
  }

 fun parseQuestionFeedback(json: JSONObject, questionId: String): QuestionFeedback =
  QuestionFeedback(
   questionId = questionId,
   isCorrect = json.getBoolean("isCorrect"),
   correctOptionId = json.optString("correctOptionId"),
   explanation = json.optString("explanation")
  )

 fun parseNodeCompletion(json: JSONObject): NodeCompletionResult {
  val nextNode = json.optJSONObject("nextUnlockedNode")?.let {
   RouteNode(
    id = it.getString("id"),
    title = it.optString("title"),
    sequence = it.optInt("sequence"),
    state = RouteNodeState.DISPONIBLE,
    activityType = ActivityType.LECCION
   )
  }
  return NodeCompletionResult(
   experienceEarned = json.optInt("experienceEarned"),
   rupiMessage = json.optString("rupiMessage").ifBlank { "¡Parada completada!" },
   badgeName = json.optJSONObject("badgeEarned")?.let { optNonNullString(it, "name") },
   isRouteCompleted = json.optBoolean("isRouteCompleted"),
   routeBonusExperience = json.optInt("routeBonusExperience"),
   nextUnlockedNode = nextNode
  )
 }

 fun parseAchievements(
  experienceJson: JSONObject,
  streakJson: JSONObject,
  leaderboardJson: JSONObject,
  badgesJson: JSONArray
 ): AchievementsOverview {
  val entriesJson = leaderboardJson.optJSONArray("entries") ?: JSONArray()
  val entries = List(entriesJson.length()) { i ->
   val entry = entriesJson.getJSONObject(i)
   LeaderboardEntry(
    rank = entry.optInt("rank"),
    displayName = entry.optString("displayName"),
    totalXp = entry.optInt("totalXp"),
    streakDays = entry.optInt("streakDays"),
    isCurrentUser = entry.optBoolean("esUsuarioActual")
   )
  }
  val badges = List(badgesJson.length()) { i ->
   val b = badgesJson.getJSONObject(i)
   StudentBadge(name = b.optString("name"), description = optNonNullString(b, "description"))
  }
  return AchievementsOverview(
   experience = ExperienceSummary(totalXp = experienceJson.optInt("totalXp")),
   streak = StreakSummary(days = streakJson.optInt("days"), atRisk = streakJson.optBoolean("atRisk")),
   leaderboard = ClassroomLeaderboard(
    classroomName = leaderboardJson.optString("classroomName"),
    myRank = leaderboardJson.optInt("myRank"),
    totalStudents = leaderboardJson.optInt("totalStudents"),
    entries = entries
   ),
   badges = badges
  )
 }

 fun parseDailyGoalDashboard(overviewJson: JSONObject, comparisonJson: JSONObject): DailyGoalDashboard {
  val today = overviewJson.getJSONObject("today")
  val weekJson = overviewJson.getJSONArray("weeklyHistory")
  val optsJson = overviewJson.getJSONArray("availableTargetOptions")
  val overview = DailyGoalOverview(
   completedLessons = today.getInt("completedLessons"),
   targetLessons = today.getInt("targetLessons"),
   pendingLessons = today.getInt("pendingLessons"),
   isGoalMet = today.getBoolean("isGoalMet"),
   motivationalMessage = today.getString("motivationalMessage"),
   currentStreakDays = overviewJson.optInt("currentStreakDays"),
   currentDailyTargetSetting = overviewJson.getInt("currentDailyTargetSetting"),
   availableTargetOptions = List(optsJson.length()) { optsJson.getInt(it) },
   weeklyHistory = List(weekJson.length()) { i ->
    val d = weekJson.getJSONObject(i)
    DailyGoalDay(
     isToday = d.getBoolean("isToday"),
     dayOfWeek = d.getString("dayOfWeek"),
     isGoalMet = d.getBoolean("isGoalMet"),
     completedLessons = d.getInt("completedLessons"),
     targetLessons = d.getInt("targetLessons")
    )
   }
  )
  val comparison = ScoreComparison(
   todayExperience = comparisonJson.getInt("todayExperience"),
   yesterdayExperience = comparisonJson.getInt("yesterdayExperience"),
   todayLessons = comparisonJson.getInt("todayLessons"),
   yesterdayLessons = comparisonJson.getInt("yesterdayLessons"),
   trend = comparisonJson.getString("trend"),
   message = comparisonJson.getString("message")
  )
  return DailyGoalDashboard(overview = overview, comparison = comparison)
 }

 fun parseDailyChallenge(json: JSONObject): DailyChallenge {
  val saved = json.optJSONArray("answeredToday")
  val answeredMap = mutableMapOf<String, QuestionFeedback>()
  if (saved != null) {
   for (i in 0 until saved.length()) {
    val ans = saved.getJSONObject(i)
    val qid = ans.getString("questionId")
    answeredMap.putIfAbsent(qid, parseQuestionFeedback(ans, qid))
   }
  }
  return DailyChallenge(
   id = json.getString("id"),
   date = json.getString("date"),
   activityVersionId = json.getString("activityVersionId"),
   skill = json.getString("skill"),
   estimatedMinutes = json.getInt("estimatedMinutes"),
   experienceReward = json.getInt("experienceReward"),
   title = json.getString("title"),
   description = json.optString("description"),
   readingText = if (json.isNull("readingText")) "" else json.getString("readingText"),
   isCompleted = json.getBoolean("isCompleted"),
   answeredToday = answeredMap,
   questions = parseLessonQuestions(json.getJSONArray("questions"))
  )
 }

 fun parsePreferences(prefsJson: JSONObject): StudyPreferences =
  StudyPreferences(
   dailyGoal = prefsJson.getInt("dailyGoal"),
   goals = prefsJson.getBoolean("goals"),
   evaluations = prefsJson.getBoolean("evaluations"),
   system = prefsJson.getBoolean("system"),
   remindAt = prefsJson.getString("remindAt"),
   quietFrom = prefsJson.getString("quietFrom"),
   quietUntil = prefsJson.getString("quietUntil"),
   revision = prefsJson.getLong("revision")
  )

 fun preferencesToJson(preferences: StudyPreferences): JSONObject =
  JSONObject()
   .put("dailyGoal", preferences.dailyGoal)
   .put("goals", preferences.goals)
   .put("evaluations", preferences.evaluations)
   .put("system", preferences.system)
   .put("remindAt", preferences.remindAt)
   .put("quietFrom", preferences.quietFrom)
   .put("quietUntil", preferences.quietUntil)
   .put("revision", preferences.revision)

 fun parseNotificationsDashboard(statusJson: JSONObject, inboxJson: JSONObject): NotificationsDashboard {
  val status = NotificationPreferencesStatus(
   completedToday = statusJson.getInt("completedToday"),
   preferences = parsePreferences(statusJson.getJSONObject("preferences"))
  )
  val itemsJson = inboxJson.getJSONArray("items")
  val inbox = NotificationInbox(
   quiet = inboxJson.optBoolean("quiet"),
   system = inboxJson.optBoolean("system"),
   items = List(itemsJson.length()) { i ->
    val item = itemsJson.getJSONObject(i)
    StudyNotificationItem(
     id = item.getString("id"),
     title = item.getString("title"),
     body = item.getString("body"),
     read = item.getBoolean("read")
    )
   }
  )
  return NotificationsDashboard(status = status, inbox = inbox)
 }

 fun parseAiPracticeStatus(json: JSONObject): AiPracticeStatus {
  val practiceJson = json.optJSONObject("practice")
  val practice = practiceJson?.let { p ->
   val questionsJson = p.getJSONArray("questions")
   AiPracticeDetail(
    practiceId = p.getString("practiceId"),
    status = p.getString("status"),
    source = p.optString("source"),
    reviewState = p.optString("reviewState"),
    correctCount = p.optInt("correct"),
    questions = List(questionsJson.length()) { i ->
     val q = questionsJson.getJSONObject(i)
     val optsJson = q.getJSONArray("options")
     AiQuizQuestion(
      id = q.getString("id"),
      text = q.getString("text"),
      options = List(optsJson.length()) { j ->
       val opt = optsJson.getJSONObject(j)
       QuestionOption(id = opt.getString("id"), text = opt.getString("text"))
      },
      selectedOptionId = if (q.isNull("selectedOption")) null else q.optString("selectedOption"),
      isCorrect = q.optBoolean("correct"),
      explanation = q.optString("explanation")
     )
    }
   )
  }
  return AiPracticeStatus(
   configured = json.optBoolean("configured"),
   reviewedAvailable = json.optBoolean("reviewedAvailable"),
   practice = practice
  )
 }

 fun parseQuestionHint(json: JSONObject): QuestionHint {
  val stepsJson = json.getJSONArray("steps")
  return QuestionHint(
   level = json.getString("level"),
   steps = List(stepsJson.length()) { stepsJson.getString(it) },
   hint = json.getString("hint"),
   realLifeExample = optNonNullString(json, "realLifeExample"),
   source = json.optString("source"),
   reinforced = json.optBoolean("reinforced")
  )
 }

 fun parseFlashcardDeck(json: JSONObject): FlashcardDeck {
  val cardsJson = json.getJSONArray("cards")
  return FlashcardDeck(
   currentCardId = json.optString("current"),
   revealed = json.optBoolean("revealed"),
   revision = json.getLong("revision"),
   cards = List(cardsJson.length()) { i ->
    val c = cardsJson.getJSONObject(i)
    FlashcardItem(
     id = c.getString("id"),
     state = c.getString("state"),
     question = c.getString("question"),
     answer = c.getString("answer"),
     source = c.getString("source")
    )
   }
  )
 }

 fun parseVisualSummaryResource(json: JSONObject): VisualSummaryResource {
  val contentJson = json.optJSONObject("content")
  val content = contentJson?.let { c ->
   val conceptsJson = c.getJSONArray("concepts")
   val stepsJson = c.getJSONArray("steps")
   VisualSummaryContent(
    title = c.getString("title"),
    concepts = List(conceptsJson.length()) { i ->
     val concept = conceptsJson.getJSONObject(i)
     VisualSummaryConcept(title = concept.getString("title"), text = concept.getString("text"))
    },
    steps = List(stepsJson.length()) { stepsJson.getString(it) },
    example = c.getString("example"),
    remember = c.getString("remember")
   )
  }
  return VisualSummaryResource(
   id = json.optString("id"),
   grade = json.optInt("grade"),
   version = json.optString("version"),
   state = json.optString("state"),
   source = json.optString("source"),
   content = content,
   rawJson = json.toString()
  )
 }

 fun parseVisualSummaryResponse(json: JSONObject): VisualSummaryLookup =
  VisualSummaryLookup(
   resource = json.optJSONObject("resource")?.let(::parseVisualSummaryResource),
   configured = json.optBoolean("configured"),
   offline = false
  )

 fun topicToJson(topic: StudyTopic): JSONObject {
  val json = JSONObject()
   .put("routeId", topic.routeId)
   .put("nodeId", topic.nodeId)
   .put("title", topic.title)
   .put("routeTitle", topic.routeTitle)
  topic.route?.let { r ->
   json.put(
    "route",
    JSONObject()
     .put("versionRouteId", r.versionRouteId)
     .put("title", r.title)
   )
  }
  return json
 }

 fun parseStudyTopic(json: JSONObject): StudyTopic {
  val routeJson = json.optJSONObject("route")
  val route = routeJson?.let {
   LearningRouteSummary(
    versionRouteId = it.optString("versionRouteId").ifBlank { json.getString("routeId") },
    title = it.optString("title").ifBlank { json.optString("routeTitle") }
   )
  }
  return StudyTopic(
   routeId = json.getString("routeId"),
   nodeId = json.getString("nodeId"),
   title = json.getString("title"),
   routeTitle = json.optString("routeTitle"),
   route = route
  )
 }
}
