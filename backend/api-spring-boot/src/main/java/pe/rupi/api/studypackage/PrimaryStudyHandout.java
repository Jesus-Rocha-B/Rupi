package pe.rupi.api.studypackage;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import pe.rupi.api.studypackage.StudyPackageModel.Unit;

/** Repaso original de Rupi, separado de las publicaciones oficiales referenciadas. */
final class PrimaryStudyHandout {
    private static final Color FOREST = new Color(0x173e30);
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, FOREST);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, FOREST);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 12, FOREST);
    private static final Font NOTE = FontFactory.getFont(FontFactory.HELVETICA, 10, FOREST);

    byte[] render(Unit unit) {
        var out = new ByteArrayOutputStream();
        var doc = new Document(PageSize.A4, 48, 48, 48, 48);
        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Repaso Rupi - " + unit.gradeName() + " - " + unit.areaName());
            doc.addAuthor("Rupi");
            doc.open();
            doc.add(new Paragraph("Mi cuaderno de repaso", TITLE));
            doc.add(new Paragraph(unit.gradeName() + " · " + unit.areaName(), LABEL));
            para(doc, "Ejercicios originales de Rupi. Repaso general del grado; no son ejercicios oficiales del MINEDU ni reemplazan las actividades de tu profesor.", NOTE);
            para(doc, "Nombre: __________________________   Fecha: ______________", BODY);
            para(doc, "Resuelve con calma. Escribe o dibuja tus ideas. Las respuestas están en la página siguiente.", BODY);
            int n = 0;
            for (var entry : unit.supplementaryResources()) {
                for (var exercise : entry.exercises()) {
                    para(doc, (++n) + ". " + exercise.prompt(), LABEL);
                    for (int line = 0; line < 4; line++) para(doc, "________________________________________________________", NOTE);
                }
            }
            doc.newPage();
            doc.add(new Paragraph("Revisamos lo aprendido", TITLE));
            para(doc, "Inténtalo antes de mirar. En las preguntas abiertas puede haber varias respuestas válidas; conversa con tu profesor.", BODY);
            n = 0;
            for (var entry : unit.supplementaryResources()) {
                for (var exercise : entry.exercises()) para(doc, (++n) + ". " + exercise.answer(), BODY);
            }
            para(doc, "Para seguir aprendiendo con el MINEDU", LABEL);
            para(doc, "Los libros oficiales se descargan por separado desde su repositorio. Necesitas internet para abrirlos; guárdalos antes de desconectarte.", NOTE);
            for (var entry : unit.supplementaryResources()) {
                para(doc, entry.title() + " · " + entry.year() + " · " + entry.publisher(), NOTE);
                para(doc, entry.sourceUrl(), NOTE);
                para(doc, entry.note(), NOTE);
            }
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo preparar el repaso de Rupi", e);
        }
        return out.toByteArray();
    }

    private static void para(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setLeading(17);
        p.setSpacingBefore(12);
        doc.add(p);
    }

    static String sources(Unit unit) {
        var html = new StringBuilder("<!doctype html><html lang=\"es\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Materiales MINEDU - Rupi</title><style>body{font:18px/1.6 system-ui;color:#173e30;background:#faf8f5;max-width:760px;margin:32px auto;padding:20px}a{color:#246a91;overflow-wrap:anywhere}li{margin:24px 0}a:focus-visible{outline:3px solid #246a91}</style><h1>Materiales del MINEDU</h1>");
        html.append("<p>").append(escape(unit.gradeName())).append(" · ").append(escape(unit.areaName())).append("</p>");
        html.append("<p>Estos enlaces necesitan internet. Abre la ficha oficial y guarda su PDF antes de desconectarte. Los libros del MINEDU no están incluidos en este ZIP.</p><p>El archivo repaso-rupi.pdf contiene ejercicios originales de Rupi y sí funciona sin conexión.</p><ul>");
        for (var entry : unit.supplementaryResources()) {
            html.append("<li><a href=\"").append(escape(entry.sourceUrl())).append("\" rel=\"noreferrer\">")
                .append(escape(entry.title())).append("</a><br>").append(escape(entry.publisher()))
                .append(" · ").append(entry.year()).append("<br>").append(escape(entry.note()))
                .append("<br>Referencia revisada: ").append(escape(entry.checkedOn())).append("</li>");
        }
        return html.append("</ul></html>").toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
