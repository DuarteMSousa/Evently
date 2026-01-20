package org.example.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.example.models.TicketInformation;

public class FileGenerationUtils {

    public static byte[] generateTicketPdf(String htmlTemplate, BufferedImage qrCode, BufferedImage logo, TicketInformation information) throws Exception {

        ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
        ImageIO.write(qrCode, "PNG", qrBaos);
        String qrBase64 = Base64.getEncoder().encodeToString(qrBaos.toByteArray());

        ByteArrayOutputStream logoBaos = new ByteArrayOutputStream();
        ImageIO.write(logo, "PNG", logoBaos);
        String logoBase64 = Base64.getEncoder().encodeToString(logoBaos.toByteArray());

        String html = htmlTemplate
                .replace("{{qrCodeBase64}}", qrBase64)
                .replace("{{logoBase64}}", logoBase64)
                .replace("{{eventName}}", information.getEventName())
                .replace("{{eventDate}}", information.getEventDate())
                .replace("{{venue}}", information.getVenueName())
                .replace("{{tier}}", information.getTier())
                .replace("{{backgroundColor}}", "#FFFFFF")
                .replace("{{textColor}}", "#000000")
                .replace("{{borderColor}}", "#FF0000");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, null);
        builder.toStream(baos);
        builder.run();

        return baos.toByteArray();
    }

    public static BufferedImage generateQRCodeImage(String text,int width,int height) throws WriterException  {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

}
