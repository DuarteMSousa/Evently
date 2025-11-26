package org.example.services;

import com.google.zxing.WriterException;
import org.example.exceptions.*;
import org.example.messages.TicketMessage;
import org.example.models.TicketInformation;
import org.example.utils.FileGenerationUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class TicketFileGenerationService {

    private final String TICKET_FILE_PATH = "TicketFiles";
    private final String IMAGES_PATH = "images";
    private final String TEMPLATES_PATH = "templates";

    public byte[] generateTicketFile(TicketMessage ticket) {
        BufferedImage qrCode;
        try {
            qrCode = FileGenerationUtils.generateQRCodeImage(ticket.getId().toString(), 150, 150);
        } catch (WriterException ex) {
            throw new QrCodeGenerationException("");
        }

        TicketInformation information = new TicketInformation();
        information.setEventName("Evento");
        information.setVenueName("Venue");
        information.setTier("Tier");
        information.setEventDate("15/12/2004");

        BufferedImage logo;
        try (InputStream logoStream = getClass().getResourceAsStream("/" +
                IMAGES_PATH + "/evently.jpg")) {
            if (logoStream == null) throw new LogoNotFoundException("Logo não encontrado");
            logo = ImageIO.read(logoStream);
        } catch (IOException e) {
            throw new LogoNotFoundException("Erro ao ler logo");
        }

        String htmlTemplate;
        try (InputStream templateStream = getClass().getResourceAsStream("/" + TEMPLATES_PATH + "/ticketTemplate.html")) {
            if (templateStream == null) throw new TemplateNotFoundException("Template HTML não encontrado");
            htmlTemplate = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TemplateNotFoundException("Erro ao ler template HTML");
        }

        byte[] file;
        try {
            file = FileGenerationUtils.generateTicketPdf(htmlTemplate, qrCode, logo, information);
        } catch (Exception e) {
            throw new FileGenerationException("");
        }

        return file;
    }

    public void saveTicketFile(TicketMessage ticket) {
        byte[] pdfBytes = generateTicketFile(ticket);
        File pdfDir = new File(TICKET_FILE_PATH);
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        File pdfFile = new File(pdfDir, ticket.getId() + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            fos.write(pdfBytes);
            fos.flush();
        } catch (IOException e) {
            throw new FileSaveException("Erro ao guardar PDF");
        }
    }

    public byte[] getTicketPdf(UUID ticketId) {
        File pdfFile = new File(TICKET_FILE_PATH + "/" + ticketId + ".pdf");
        if (!pdfFile.exists()) {
            throw new TicketFileNotFoundException("PDF não encontrado para o ticket " + ticketId);
        }
        try (FileInputStream fis = new FileInputStream(pdfFile)) {
            return fis.readAllBytes();
        } catch (IOException e) {
            throw new TicketFileNotFoundException("Erro ao ler PDF");
        }
    }
}
