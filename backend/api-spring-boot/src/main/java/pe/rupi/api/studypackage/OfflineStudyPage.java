package pe.rupi.api.studypackage;

import java.util.List;
import java.util.Map;
import pe.rupi.api.studypackage.StudyPackageModel.Unit;

/** Documento autónomo: no usa scripts, fuentes, estilos ni servicios remotos. */
final class OfflineStudyPage {
    private OfflineStudyPage() {}

    static String render(Unit unit, Map<String, String> files, List<String> missing) {
        var html = new StringBuilder("""
                <!doctype html><html lang="es"><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; img-src 'self' data:; media-src 'self'; base-uri 'none'; form-action 'none'">
                <title>Rupi · Mi mochila sin conexión</title>
                <style>
                *{box-sizing:border-box}body{margin:0;background:#faf8f5;color:#173e30;font:18px/1.6 system-ui,sans-serif}
                main{max-width:850px;margin:auto;padding:24px}h1{line-height:1.2}h2{color:#31724b}
                section{border-top:3px solid #f95738;margin-top:24px;padding-top:12px}
                a{color:#246a91;overflow-wrap:anywhere;display:inline-block;min-height:48px;padding:10px 4px}
                :focus-visible{outline:3px solid #246a91;outline-offset:3px}p{white-space:pre-wrap;overflow-wrap:anywhere}
                .note{border-left:6px solid #f95738;background:#fff3ce;padding:16px}
                details{margin:16px 0;border:2px solid #31724b;border-radius:12px;padding:12px}
                summary{cursor:pointer;min-height:48px;font-weight:bold}.answer{border-bottom:1px solid #31724b;height:60px}
                @media print{details{break-inside:avoid}.answer{height:80px}main{padding:0}}
                </style><main><header><strong>RUPI · MI MOCHILA SIN CONEXIÓN</strong>
                """);
        html.append("<h1>").append(escape(unit.unitTitle())).append("</h1><p>")
                .append(escape(unit.routeTitle() + " · " + unit.gradeName() + " · " + unit.areaName()))
                .append("</p></header><p>Ya puedes leer y practicar sin internet. Responde en tu cuaderno. Este repaso no guarda respuestas ni cambia tu avance en Rupi.</p>")
                .append("<a href=\"guia-de-estudio.pdf\">Abrir la guía PDF para imprimir</a>");
        if (!missing.isEmpty()) {
            html.append("<aside class=\"note\"><strong>Estos archivos no pudieron incluirse. Pídeselos a tu profesor:</strong><ul>");
            missing.forEach(item -> html.append("<li>").append(escape(item)).append("</li>"));
            html.append("</ul></aside>");
        }
        for (var stop : unit.stops()) {
            html.append("<section><h2>Parada ").append(stop.sequence()).append(" · ").append(escape(stop.title())).append("</h2>");
            paragraph(html, stop.instructions());
            for (var block : stop.blocks()) {
                paragraph(html, block.text());
                paragraph(html, block.accessibleText());
                if (block.fileId() != null) fileLink(html, files, block.fileId(), "Abrir material de la lectura");
            }
            for (var question : stop.questions()) {
                html.append("<h3>").append(question.sequence()).append(". ").append(escape(question.statement())).append("</h3><ul>");
                for (var option : question.options()) html.append("<li>").append(escape(option.text())).append("</li>");
                html.append("</ul><p>Escribe tu respuesta en tu cuaderno.</p><details><summary>Revisar después de responder</summary>");
                boolean correct = false;
                for (var option : question.options()) if (option.correct()) { paragraph(html, option.text()); correct = true; }
                if (!correct) paragraph(html, "Respuesta libre: revísala con tu profesor.");
                paragraph(html, question.explanation());
                html.append("</details>");
            }
            for (var resource : stop.resources()) {
                if (resource.fileId() != null) fileLink(html, files, resource.fileId(), resource.title());
                else {
                    paragraph(html, resource.title() + " · Recurso externo: necesita internet; no está incluido.");
                    // Mostrar la URL como texto evita ejecutar esquemas o HTML introducidos por contenido externo.
                    paragraph(html, resource.url());
                }
            }
            html.append("</section>");
        }
        if (!unit.supplementaryResources().isEmpty()) html.append("""
                <section><h2>Un poco más de práctica</h2>
                <a href="repaso-rupi.pdf">Abrir ejercicios de repaso Rupi (sin internet)</a>
                <p>Ejercicios originales de Rupi para tu grado. No son una publicación del MINEDU.</p>
                <a href="fuentes-minedu.html">Consultar referencias del MINEDU</a>
                <p>Los libros del MINEDU se descargan aparte desde su sitio oficial mientras tengas conexión.</p></section>
                """);
        return html.append("</main></html>").toString();
    }

    private static void fileLink(StringBuilder html, Map<String, String> files, String id, String title) {
        var path = files.get(id);
        if (path == null) paragraph(html, title + ": no incluido. Pídeselo a tu profesor.");
        else html.append("<p><a download href=\"").append(escape(path)).append("\">").append(escape(title)).append(" (archivo incluido)</a></p>");
    }

    private static void paragraph(StringBuilder html, String text) {
        if (text != null && !text.isBlank()) html.append("<p>").append(escape(text)).append("</p>");
    }

    static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
