package com.sima.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.entity.ReporteGenerado;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    public byte[] generateReportPdf(ReporteGenerado reporte, List<RegistroToma> tomas, List<Alerta> alertas) {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(27, 43, 58));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(100, 116, 139));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 95, 122));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(27, 43, 58));
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));
            Font whiteBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // Title / Header
            Paragraph title = new Paragraph("SiMA — Monitoreo Inteligente", titleFont);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph("Reporte de Control y Seguimiento Clínico", subtitleFont);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Report Details Metadata Table
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(20);
            
            addMetaCell(metaTable, "Nombre del Reporte:", reporte.getNombre(), boldFont, normalFont);
            addMetaCell(metaTable, "Tipo de Rango:", reporte.getTipo(), boldFont, normalFont);
            addMetaCell(metaTable, "Categoría:", reporte.getTipoReporte(), boldFont, normalFont);
            String paciente = reporte.getAdultoMayorNombre() != null ? reporte.getAdultoMayorNombre() : "Todos los Adultos Mayores";
            addMetaCell(metaTable, "Adulto Mayor:", paciente, boldFont, normalFont);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String fechaGen = reporte.getFechaGeneracion() != null ? reporte.getFechaGeneracion().format(formatter) : java.time.LocalDateTime.now().format(formatter);
            addMetaCell(metaTable, "Generado el:", fechaGen, boldFont, normalFont);
            addMetaCell(metaTable, "Generado por:", reporte.getGeneradoPor(), boldFont, normalFont);
            
            if (reporte.getFechaInicio() != null && reporte.getFechaFin() != null) {
                DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                String rango = reporte.getFechaInicio().format(df) + " al " + reporte.getFechaFin().format(df);
                addMetaCell(metaTable, "Período de Análisis:", rango, boldFont, normalFont);
            } else {
                addMetaCell(metaTable, "Período de Análisis:", "Histórico General", boldFont, normalFont);
            }
            
            document.add(metaTable);

            // Statistics Summary Cards (using a table layout)
            document.add(new Paragraph("Resumen de Métricas", sectionFont));
            Paragraph spacing = new Paragraph(" ");
            spacing.setSpacingAfter(4);
            document.add(spacing);

            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(20);

            // Calculate metrics
            long totalTomas = tomas.size();
            long tomasTomadas = tomas.stream().filter(t -> "tomado".equals(t.getEstado()) || "confirmado_manual".equals(t.getEstado())).count();
            int cumplimiento = totalTomas > 0 ? (int) ((tomasTomadas * 100) / totalTomas) : 0;
            long totalAlertas = alertas.size();
            long alertasActivas = alertas.stream().filter(a -> !a.getResuelta()).count();

            addStatCard(statsTable, "Cumplimiento", cumplimiento + "%", new Color(82, 183, 136));
            addStatCard(statsTable, "Tomas Programadas", String.valueOf(totalTomas), new Color(46, 134, 171));
            addStatCard(statsTable, "Alertas Generadas", String.valueOf(totalAlertas), new Color(231, 111, 81));
            addStatCard(statsTable, "Alertas Activas", String.valueOf(alertasActivas), new Color(192, 69, 42));

            document.add(statsTable);

            // Section 1: Medication Intake Log
            if ("General".equals(reporte.getTipoReporte()) || "Medicacion".equals(reporte.getTipoReporte())) {
                document.add(new Paragraph("Historial de Cumplimiento de Medicación", sectionFont));
                document.add(spacing);

                if (tomas.isEmpty()) {
                    Paragraph emptyMsg = new Paragraph("No se registran tomas de medicamento para los criterios seleccionados en este período.", normalFont);
                    emptyMsg.setSpacingAfter(15);
                    document.add(emptyMsg);
                } else {
                    PdfPTable tomasTable = new PdfPTable(5);
                    tomasTable.setWidthPercentage(100);
                    tomasTable.setWidths(new float[]{30f, 25f, 15f, 15f, 15f});
                    tomasTable.setSpacingAfter(20);

                    // Headers
                    addTableHeader(tomasTable, "Adulto Mayor", whiteBoldFont);
                    addTableHeader(tomasTable, "Medicamento", whiteBoldFont);
                    addTableHeader(tomasTable, "Hora Programada", whiteBoldFont);
                    addTableHeader(tomasTable, "Hora Registro", whiteBoldFont);
                    addTableHeader(tomasTable, "Estado", whiteBoldFont);

                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    for (RegistroToma t : tomas) {
                        addTableCell(tomasTable, t.getAdulto().getNombre() + " " + t.getAdulto().getApellido(), normalFont);
                        addTableCell(tomasTable, t.getHorario().getMedicamento().getNombre() + " (" + t.getHorario().getMedicamento().getDosis() + ")", normalFont);
                        addTableCell(tomasTable, t.getFechaHoraProgramada().format(timeFormatter), normalFont);
                        addTableCell(tomasTable, t.getFechaHoraRegistro() != null ? t.getFechaHoraRegistro().format(timeFormatter) : "—", normalFont);
                        
                        // Status colored badge text
                        String estado = t.getEstado() != null ? t.getEstado().toUpperCase() : "PENDIENTE";
                        Color stateColor = new Color(100, 116, 139); // Gray
                        if ("TOMADO".equals(estado) || "CONFIRMADO_MANUAL".equals(estado)) {
                            stateColor = new Color(26, 122, 74); // Green
                        } else if ("OMITIDO".equals(estado)) {
                            stateColor = new Color(192, 69, 42); // Red
                        }
                        Font stateFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, stateColor);
                        addTableCell(tomasTable, estado, stateFont);
                    }
                    document.add(tomasTable);
                }
            }

            // Section 2: Alerts Log
            if ("General".equals(reporte.getTipoReporte()) || "Alertas".equals(reporte.getTipoReporte())) {
                document.add(new Paragraph("Historial de Alertas e Incidentes", sectionFont));
                document.add(spacing);

                if (alertas.isEmpty()) {
                    Paragraph emptyMsg = new Paragraph("No se registran alertas o incidentes críticos para los criterios seleccionados en este período.", normalFont);
                    emptyMsg.setSpacingAfter(15);
                    document.add(emptyMsg);
                } else {
                    PdfPTable alertasTable = new PdfPTable(4);
                    alertasTable.setWidthPercentage(100);
                    alertasTable.setWidths(new float[]{25f, 20f, 40f, 15f});
                    alertasTable.setSpacingAfter(20);

                    // Headers
                    addTableHeader(alertasTable, "Adulto Mayor", whiteBoldFont);
                    addTableHeader(alertasTable, "Tipo de Alerta", whiteBoldFont);
                    addTableHeader(alertasTable, "Mensaje de Alerta", whiteBoldFont);
                    addTableHeader(alertasTable, "Estado", whiteBoldFont);

                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    for (Alerta a : alertas) {
                        addTableCell(alertasTable, a.getAdulto().getNombre() + " " + a.getAdulto().getApellido(), normalFont);
                        
                        // Human readable alert type
                        String tipoHuman = a.getTipoAlerta();
                        if ("omision_medicacion".equals(tipoHuman)) tipoHuman = "Omisión de Medicamento";
                        else if ("caida_detectada".equals(tipoHuman)) tipoHuman = "Caída Detectada";
                        else if ("emergencia".equals(tipoHuman)) tipoHuman = "Emergencia S.O.S";
                        else if ("bateria_baja".equals(tipoHuman)) tipoHuman = "Batería Baja Pulsera";
                        
                        addTableCell(alertasTable, tipoHuman + "\n" + a.getCreadoEn().format(timeFormatter), normalFont);
                        addTableCell(alertasTable, a.getMensaje(), normalFont);
                        
                        String res = a.getResuelta() ? "RESUELTA" : "ACTIVA";
                        Color stateColor = a.getResuelta() ? new Color(26, 122, 74) : new Color(192, 69, 42);
                        Font stateFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, stateColor);
                        addTableCell(alertasTable, res, stateFont);
                    }
                    document.add(alertasTable);
                }
            }

            // Footer / Disclaimer
            Paragraph footer = new Paragraph("Este documento es un reporte automatizado generado por el Sistema Inteligente de Monitoreo para Adultos (SiMA). La información contenida tiene fines de asistencia y monitoreo, no sustituye el diagnóstico o consejo de un profesional de la salud.", FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, new Color(100, 116, 139)));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void addMetaCell(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell c1 = new PdfPCell(new Paragraph(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Paragraph(value, valFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(4);
        table.addCell(c2);
    }

    private void addStatCard(PdfPTable table, String title, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pTitle = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139)));
        pTitle.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pTitle);

        Paragraph pVal = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, color));
        pVal.setAlignment(Element.ALIGN_CENTER);
        pVal.setSpacingBefore(4);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String headerTitle, Font font) {
        PdfPCell header = new PdfPCell(new Paragraph(headerTitle, font));
        header.setBackgroundColor(new Color(30, 41, 59));
        header.setPadding(6);
        header.setHorizontalAlignment(Element.ALIGN_LEFT);
        header.setBorderColor(new Color(51, 65, 85));
        table.addCell(header);
    }

    private void addTableCell(PdfPTable table, String cellValue, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(cellValue, font));
        cell.setPadding(6);
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }
}
