package org.example.services;

import com.google.zxing.WriterException;
import org.example.messages.TicketGeneratedMessage;
import org.example.exceptions.*;
import org.example.models.TicketInformation;
import org.example.utils.FileGenerationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
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

    private Logger logger = LoggerFactory.getLogger(TicketFileGenerationService.class);

    private static final Marker TICKET_FILE_GENERATION = MarkerFactory.getMarker("TICKET_FILE_GENERATION");
    private static final Marker TICKET_FILE_SAVE = MarkerFactory.getMarker("TICKET_FILE_SAVE");
    private static final Marker TICKET_FILE_GET = MarkerFactory.getMarker("TICKET_FILE_GET");

    private byte[] generateTicketFile(TicketGeneratedMessage ticket) {
        logger.info(TICKET_FILE_GENERATION, "generateTicketFile method entered");

        BufferedImage qrCode;
        try {
            qrCode = FileGenerationUtils.generateQRCodeImage(ticket.getId().toString(), 150, 150);
        } catch (WriterException ex) {
            logger.error(TICKET_FILE_GENERATION, "Error generating QR Code Image: {}", ex.getMessage());
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
            if (logoStream == null) {
                logger.error(TICKET_FILE_GENERATION, "LOGO not found");
                throw new LogoNotFoundException("Logo not found");
            }
            logo = ImageIO.read(logoStream);
        } catch (IOException e) {
            logger.error(TICKET_FILE_GENERATION, "Error loading logo");
            throw new LogoNotFoundException("Error loading logo");
        }

        String htmlTemplate;
        try (InputStream templateStream = getClass().getResourceAsStream("/" + TEMPLATES_PATH + "/ticketTemplate.html")) {
            if (templateStream == null) {
                logger.error(TICKET_FILE_GENERATION, "Template not found");
                throw new TemplateNotFoundException("Template not found");
            }
            htmlTemplate = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(TICKET_FILE_GENERATION, "Error loading template: {}", e.getMessage());
            throw new TemplateNotFoundException("Error loading template");
        }

        byte[] file;
        try {
            file = FileGenerationUtils.generateTicketPdf(htmlTemplate, qrCode, logo, information);
        } catch (Exception e) {
            logger.error(TICKET_FILE_GENERATION, "Error generating file: {}", e.getMessage());
            throw new FileGenerationException("Error generating file");
        }

        return file;
    }

    public void saveTicketFile(TicketGeneratedMessage ticket) {
        logger.info(TICKET_FILE_SAVE, "saveTicketFile method entered");
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
            logger.error(TICKET_FILE_SAVE, "Error saving file: {}", e.getMessage());
            throw new FileSaveException("Error saving file");
        }
    }

    public byte[] getTicketPdf(UUID ticketId) {
        logger.info(TICKET_FILE_GET, "getTicketPdf method entered");
        File pdfFile = new File(TICKET_FILE_PATH + "/" + ticketId + ".pdf");
        if (!pdfFile.exists()) {
            logger.error(TICKET_FILE_GET, "Ticket file not found: {}", ticketId.toString());
            throw new TicketFileNotFoundException("Ticket file not found");
        }
        try (FileInputStream fis = new FileInputStream(pdfFile)) {
            return fis.readAllBytes();
        } catch (IOException e) {
            logger.error(TICKET_FILE_GET, "Error reading file: {}", e.getMessage());
            throw new TicketFileNotFoundException("Error loading pdf");
        }
    }
}
