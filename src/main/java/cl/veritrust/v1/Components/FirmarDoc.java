package cl.veritrust.v1.Components;

import cl.veritrust.v1.Model.Documento;
import cl.veritrust.v1.Service.DocumentoService;
import cl.veritrust.v1.Service.FileStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

@Component
public class FirmarDoc {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private DocumentoService documentoService;

    @Value("${app.libreoffice.path:soffice}")
    private String libreofficeCmd;

    public Documento signDocumento(Documento documento) {
        try {
            String tipo = documento.getTipoContenido();
            String stored = documento.getNombreAlmacenado();
            File sourceFile = fileStorageService.loadFileAsResource(stored).getFile();

            if (tipo != null && tipo.toLowerCase().contains("officedocument.wordprocessingml.document")) {
                sourceFile = convertDocxToPdf(sourceFile);
                String pdfStoredName = storeConvertedPdf(sourceFile, documento);
                documento.setNombreAlmacenado(pdfStoredName);
                documento.setTipoContenido("application/pdf");
            }

            return signVisualPdf(documento);
        } catch (IOException ex) {
            throw new RuntimeException("Error en proceso de firmado: " + ex.getMessage(), ex);
        }
    }

    private Documento signVisualPdf(Documento documento) throws IOException {
        var resource = fileStorageService.loadFileAsResource(documento.getNombreAlmacenado());
        File originalFile = resource.getFile();

        try (PDDocument pdDocument = PDDocument.load(originalFile)) {
            for (PDPage page : pdDocument.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                float x = mediaBox.getLowerLeftX() + 40;
                float y = mediaBox.getLowerLeftY() + 40;

                try (PDPageContentStream contentStream = new PDPageContentStream(pdDocument, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    contentStream.newLineAtOffset(x, y);
                    contentStream.showText("Firmado por: VeriTrust");
                    contentStream.endText();
                }
            }

            String originalStored = documento.getNombreAlmacenado();
            String signedFileName = makeSignedName(originalStored);
            Path signedPath = fileStorageService.getUploadDir().resolve(signedFileName);
            pdDocument.save(signedPath.toFile());

            documento.setFirmado(true);
            documento.setNombreFirmado(signedFileName);
            documentoService.CrearDocumento(documento);
            return documento;
        }
    }

    private File convertDocxToPdf(File docxFile) throws IOException {
        File outDir = fileStorageService.getUploadDir().toFile();
        ProcessBuilder pb = new ProcessBuilder(libreofficeCmd, "--headless", "--convert-to", "pdf", "--outdir", outDir.getAbsolutePath(), docxFile.getAbsolutePath());
        Process p = pb.start();
        try {
            int rc = p.waitFor();
            if (rc != 0) {
                throw new IOException("Error al convertir DOCX a PDF. Código: " + rc);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Conversión interrumpida", e);
        }

        String name = docxFile.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        File pdf = new File(outDir, base + ".pdf");
        if (!pdf.exists()) throw new IOException("PDF convertido no encontrado: " + pdf.getAbsolutePath());
        return pdf;
    }

    private String storeConvertedPdf(File pdfFile, Documento documento) throws IOException {
        String newName = UUID.randomUUID().toString() + "_" + pdfFile.getName();
        Path target = fileStorageService.getUploadDir().resolve(newName);
        try (InputStream in = new FileInputStream(pdfFile); OutputStream out = new FileOutputStream(target.toFile())) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
        return newName;
    }

    private String makeSignedName(String originalStored) {
        if (originalStored == null) return null;
        int dot = originalStored.lastIndexOf('.');
        if (dot > 0) {
            String base = originalStored.substring(0, dot);
            String ext = originalStored.substring(dot);
            return base + "_Firmado" + ext;
        }
        return originalStored + "_Firmado.pdf";
    }

    
}
