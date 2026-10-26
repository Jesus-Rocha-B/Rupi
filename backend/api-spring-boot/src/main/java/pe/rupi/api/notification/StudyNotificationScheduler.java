package pe.rupi.api.notification;
import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @EnableScheduling @ConditionalOnProperty(name="rupi.study-notifications.enabled",havingValue="true",matchIfMissing=true)
public class StudyNotificationScheduler {
 private final StudyNotificationService service;
 public StudyNotificationScheduler(StudyNotificationService service){this.service=service;}
 @Scheduled(fixedDelayString="${rupi.study-notifications.poll-ms:60000}",initialDelayString="${rupi.study-notifications.initial-delay-ms:30000}")
 public void run(){
  try {for(String user:service.configuredStudents())try {service.generate(user);}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo preparar un aviso personal; se reintentará.");}}
  catch(org.springframework.dao.DataAccessException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudieron consultar preferencias. Comprueba V26 y conexión.");}
 }
}
