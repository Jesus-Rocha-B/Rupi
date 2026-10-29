package pe.rupi.mobile

import android.app.*
import android.app.job.*
import android.content.*

import android.security.keystore.*
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

/** Only a session cookie is encrypted. Passwords and provider keys are never stored. */
object StudyAlerts {
 const val JOB = 2501
 private const val KEY = "rupi-study-session"
 private const val CHANNEL = "study-reminders"
 private fun key(): java.security.Key {
  val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  if(!store.containsAlias(KEY)) KeyGenerator.getInstance("AES","AndroidKeyStore").apply {
   init(KeyGenParameterSpec.Builder(KEY,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
   generateKey()
  }
  return store.getKey(KEY,null)
 }
 private fun file(context:Context)=java.io.File(context.filesDir,"study-session")
 fun enable(context:Context,api:RupiApi) {
  val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
  val payload=api.exportSession().toString().toByteArray(Charsets.UTF_8)
  val out=java.io.File(context.filesDir,"study-session.tmp")
  out.writeBytes(cipher.iv+cipher.doFinal(payload))
  java.nio.file.Files.move(out.toPath(),file(context).toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)
  val job=JobInfo.Builder(JOB,ComponentName(context,StudyAlertJob::class.java))
   .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build()
  check((context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(job)==JobScheduler.RESULT_SUCCESS) { "Android no pudo programar los avisos." }
 }
 fun restore(context:Context):RupiApi? {
  if(!file(context).exists())return null
  return try {
   val bytes=file(context).readBytes();val cipher=Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
   val value=JSONObject(cipher.doFinal(bytes.copyOfRange(12,bytes.size)).toString(Charsets.UTF_8))
   if(value.getLong("expires")<=System.currentTimeMillis()){disable(context);null}
   else RupiApi(value.getString("server")).apply { importSession(value) }
  }catch(_:Exception){disable(context);null}
 }
 fun generation(context:Context):String = try { android.util.Base64.encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(file(context).readBytes()),android.util.Base64.NO_WRAP) } catch(_:Exception){""}
 fun disable(context:Context) {
  (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).cancel(JOB)
  file(context).delete()
  (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
 }
 /** HU-43: si la cuenta se abrió en otro lugar, avisa una vez que los recordatorios quedaron en pausa. */
 fun paused(context:Context,reason:String) {
  if(!allowed(context))return
  val pending=PendingIntent.getActivity(context,JOB,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notification=Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle("Tus avisos de Rupi están en pausa").setContentText(reason)
   .setStyle(Notification.BigTextStyle().bigText(reason)).setAutoCancel(true)
   .setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(pending).build()
  (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify("sesion-unica",0,notification)
 }
 fun allowed(context:Context):Boolean {
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  manager.createNotificationChannel(NotificationChannel(CHANNEL,"Recordatorios de estudio",NotificationManager.IMPORTANCE_DEFAULT))
  return manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL).importance!=NotificationManager.IMPORTANCE_NONE
 }
 fun deliver(context:Context,api:RupiApi,active:()->Boolean) {
  if(!allowed(context))return
  val inbox=JSONObject(api.request("/api/v1/student/notifications"))
  if(inbox.optBoolean("quiet")||!inbox.optBoolean("system"))return
  val items=inbox.getJSONArray("items")
  for(i in 0 until items.length()) {
   if(!active()||!file(context).exists())return
   val item=items.getJSONObject(i)
   if(item.optBoolean("read"))continue
   val id=item.getString("id")
   if(!JSONObject(api.request("/api/v1/student/notifications/$id/claim","POST")).optBoolean("claimed"))continue
   if(!active()||!file(context).exists())return
   val intent=Intent(context,MainActivity::class.java).putExtra("notifications",true)
   val pending=PendingIntent.getActivity(context,JOB,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val notification=Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentTitle(item.getString("title")).setContentText(item.getString("body"))
    .setStyle(Notification.BigTextStyle().bigText(item.getString("body"))).setAutoCancel(true)
    .setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(pending).build()
   (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id,0,notification)
  }
 }
}
class StudyAlertJob:JobService() {
 @Volatile private var active=false
 override fun onStartJob(params:JobParameters):Boolean {
  active=true
  val generation=StudyAlerts.generation(this)
  Thread {
   var retry=false
   try { StudyAlerts.restore(this)?.let { StudyAlerts.deliver(this,it){active && generation==StudyAlerts.generation(this)} } }
   catch(e:RupiSessionExpired){if(generation==StudyAlerts.generation(this)){StudyAlerts.disable(this);if(e.otherDevice)StudyAlerts.paused(this,e.message.orEmpty())}}
   catch(_:Exception){retry=true}
   finally { if(active)jobFinished(params,retry) }
  }.start()
  return true
 }
 override fun onStopJob(params:JobParameters):Boolean { active=false;return true }
}
