package pe.rupi.api.visualsummary;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
public final class SummaryDocument {
 private SummaryDocument(){}
 static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 public static String html(SummaryService.View v){
  var d=v.content();if(d==null)throw new IllegalArgumentException();
  var b=new StringBuilder("""
   <!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
   <title>Resumen visual · Rupi</title><style>
   body{font:18px/1.6 system-ui,sans-serif;background:#faf8f5;color:#173e30;margin:0;padding:24px}
   main{max-width:850px;margin:auto}article{border:2px solid #b7cbb2;border-radius:16px;padding:20px;margin:16px 0;background:white}
   h1,h2{line-height:1.3}aside{background:#fff1c7;padding:20px;border-left:5px solid #b98628}
   .steps li{padding:12px}.meta{overflow-wrap:anywhere;font-size:15px}pre{white-space:pre-wrap}
   @media print{body{background:white}} </style><main>
   <p>Rupi · Copia descargada · Contenido asistido por IA</p>
   <aside id="status">Esta copia puede estar desactualizada. Cuando tengas conexión, vuelve al tema en Rupi para comprobar su versión y estado de revisión. Una retirada no borra una copia sin conexión.</aside>
   """);
  b.append("<h1>").append(escape(d.title())).append("</h1><p>").append(v.grade()).append(".º de primaria · ").append(escape(v.area())).append("</p>");
  b.append("<p>").append(escape(v.state().equals("REVISADO_DOCENTE")?"Revisado por docente":"Validación automática: puede contener errores; no sustituye la revisión docente")).append("</p>");
  for(var section:d.concepts())b.append("<article><h2>").append(escape(section.title())).append("</h2><p>").append(escape(section.text())).append("</p></article>");
  b.append("<h2>Paso a paso</h2><ol class='steps'>");for(var step:d.steps())b.append("<li>").append(escape(step)).append("</li>");
  b.append("</ol><article><h2>Ejemplo explicado</h2><p>").append(escape(d.example())).append("</p></article><aside><h2>Para recordar</h2><p>").append(escape(d.remember())).append("</p></aside>");
  b.append("<p class='meta'>Tema: ").append(escape(v.topic())).append("<br>Fuente: ").append(escape(v.source())).append("<br>Versión: ").append(escape(v.version())).append("<br>Proveedor/modelo: ").append(escape(v.provider()+" / "+v.model())).append("</p></main></html>");
  return b.toString();
 }
 public static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
}
