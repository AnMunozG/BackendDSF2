package cl.veritrust.v1.Components;

import cl.veritrust.v1.Model.Documento;
import cl.veritrust.v1.Service.DocumentoService;
import cl.veritrust.v1.Service.FileStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

@Component
public class FirmarDoc {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private DocumentoService documentoService;

    @Value("${app.keystore.path:}")
    private String keystorePath;

    @Value("${app.keystore.password:}")
    private String keystorePassword;

    @Value("${app.keystore.alias:}")
    private String keystoreAlias;

    @Value("${app.libreoffice.path:soffice}")
    private String libreofficeCmd;

    /**
     * Firma documento. Modo: "visual" (marca visible) o "digital" (firma PKCS#7)
     * @param documento documento
     * @param signer nombre de quien firma
     * @param modo "visual" o "digital"
     * @return documento actualizado
     */
    public Documento signDocumento(Documento documento, String signer, String modo) {
        if (modo == null) modo = "visual";
        modo = modo.toLowerCase(Locale.ROOT);
        try {
            // si es DOCX y se solicita firmado (digital o visual) convertir primero a PDF
            String tipo = documento.getTipoContenido();
            String stored = documento.getNombreAlmacenado();
            File sourceFile = fileStorageService.loadFileAsResource(stored).getFile();

            if (tipo != null && tipo.toLowerCase().contains("officedocument.wordprocessingml.document")) {
                sourceFile = convertDocxToPdf(sourceFile);
                // actualizar documento temporalmente para apuntar al PDF convertido
                String pdfStoredName = storeConvertedPdf(sourceFile, documento);
                documento.setNombreAlmacenado(pdfStoredName);
                documento.setTipoContenido("application/pdf");
            }

            if (modo.equals("visual")) {
                return signVisualPdf(documento, signer);
            } else if (modo.equals("digital")) {
                return signDigitalPdf(documento, signer);
            } else {
                throw new IllegalArgumentException("Modo desconocido: " + modo);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error en proceso de firmado: " + ex.getMessage(), ex);
        }
    }

    private Documento signVisualPdf(Documento documento, String signer) throws IOException {
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
                    contentStream.showText("Firmado por: " + signer);
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

    private Documento signDigitalPdf(Documento documento, String signer) {
        // require keystore
        if (keystorePath == null || keystorePath.isBlank() || keystorePassword == null) {
            throw new IllegalStateException("Keystore no configurado (app.keystore.path y app.keystore.password)");
        }

        try {
            // load keystore
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream is = new FileInputStream(keystorePath)) {
                ks.load(is, keystorePassword.toCharArray());
            }
            String alias = keystoreAlias;
            if (alias == null || alias.isBlank()) {
                Enumeration<String> aliases = ks.aliases();
                if (aliases.hasMoreElements()) alias = aliases.nextElement();
            }
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, keystorePassword.toCharArray());
            Certificate[] certChain = ks.getCertificateChain(alias);

            var resource = fileStorageService.loadFileAsResource(documento.getNombreAlmacenado());
            File srcFile = resource.getFile();
            String signedFileName = makeSignedName(documento.getNombreAlmacenado());
            Path signedPath = fileStorageService.getUploadDir().resolve(signedFileName);

            // perform signing with PDFBox and BouncyCastle
            try (PDDocument doc = PDDocument.load(srcFile)) {
                PDSignature signature = new PDSignature();
                signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
                signature.setName(signer);
                signature.setLocation("");
                signature.setReason("Firma digital");
                signature.setSignDate(Calendar.getInstance());

                SignatureOptions options = new SignatureOptions();
                options.setPreferredSignatureSize(8192);

                doc.addSignature(signature, new SignatureImpl(privateKey, certChain), options);

                try (FileOutputStream fos = new FileOutputStream(signedPath.toFile())) {
                    doc.saveIncremental(fos);
                }
            }

            documento.setFirmado(true);
            documento.setNombreFirmado(signedFileName);
            documentoService.CrearDocumento(documento);
            return documento;
        } catch (Exception ex) {
            throw new RuntimeException("Error firmando digitalmente: " + ex.getMessage(), ex);
        }
    }

    private File convertDocxToPdf(File docxFile) throws IOException {
        // use LibreOffice headless conversion; requires soffice in PATH or configured path
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

        // output filename is same base with .pdf
        String name = docxFile.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        File pdf = new File(outDir, base + ".pdf");
        if (!pdf.exists()) throw new IOException("PDF convertido no encontrado: " + pdf.getAbsolutePath());
        return pdf;
    }

    private String storeConvertedPdf(File pdfFile, Documento documento) throws IOException {
        // move/copy the converted pdf into uploadDir with unique prefix
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
            return base + "_signed" + ext;
        }
        return originalStored + "_signed.pdf";
    }

    // SignatureInterface implementation for PDFBox that uses BouncyCastle CMS
    private static class SignatureImpl implements org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface {
        private final PrivateKey privateKey;
        private final Certificate[] certChain;

        SignatureImpl(PrivateKey privateKey, Certificate[] certChain) {
            this.privateKey = privateKey;
            this.certChain = certChain;
        }

        @Override
        public byte[] sign(InputStream content) throws IOException {
            try {
                // read content bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = content.read(buffer)) > 0) baos.write(buffer, 0, n);
                byte[] contentBytes = baos.toByteArray();

                // build CMS signed data
                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

                List<java.security.cert.Certificate> certList = new ArrayList<>();
                for (Certificate c : certChain) certList.add(c);
                JcaCertStore certs = new JcaCertStore(certList);

                ContentSigner shaSigner = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(privateKey);
                DigestCalculatorProvider digCalcProv = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();

                CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                X509Certificate cert0 = (X509Certificate) certChain[0];
                gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(digCalcProv).build(shaSigner, cert0));
                gen.addCertificates(certs);

                CMSTypedData msg = new CMSProcessableByteArray(contentBytes);
                CMSSignedData signed = gen.generate(msg, false);
                return signed.getEncoded();
            } catch (Exception e) {
                throw new IOException("Error generando CMS: " + e.getMessage(), e);
            }
        }
    }
}
