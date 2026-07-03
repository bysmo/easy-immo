package com.easyimmo.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGeneratorService {

    private final TemplateEngine templateEngine;

    /**
     * Génère un PDF en mémoire à partir d'un template Thymeleaf et de variables de contexte
     */
    public byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) {
        log.info("Lancement de la génération du PDF pour le template : {}", templateName);

        // 1. Charger le contexte Thymeleaf
        Context context = new Context();
        context.setVariables(data);

        // 2. Compiler le HTML
        String htmlContent = templateEngine.process(templateName, context);

        // 3. Convertir le HTML en PDF binaire via Flying Saucer
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            
            // Flying Saucer nécessite un code HTML Strict bien formé (XML compliant)
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            
            log.info("Génération du PDF réussie.");
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la compilation PDF Flying Saucer : {}", e.getMessage());
            throw new RuntimeException("Échec de la génération du fichier PDF à partir du template HTML", e);
        }
    }
}
