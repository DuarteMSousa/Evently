package org.example.services;

import com.google.zxing.WriterException;
import feign.FeignException;
import org.example.clients.EventsClient;
import org.example.clients.VenuesClient;
import org.example.dtos.*;
import org.example.messages.TicketGeneratedMessage;
import org.example.exceptions.*;
import org.example.models.TicketInformation;
import org.example.publishers.FileGenerationMessagesPublisher;
import org.example.utils.FileGenerationUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    FileGenerationMessagesPublisher fileGenerationMessagesPublisher;

    @Autowired
    EventsClient eventsClient;

    @Autowired
    private VenuesClient venuesClient;

    /**
     * Generates a ticket PDF file in memory.
     *
     * @param ticket ticket generation message (must include id)
     * @return generated PDF bytes
     * @throws QrCodeGenerationException if QR code generation fails
     * @throws LogoNotFoundException     if the logo cannot be loaded from classpath
     * @throws TemplateNotFoundException if the HTML template cannot be loaded from classpath
     * @throws FileGenerationException   if PDF generation fails
     */
    private byte[] generateTicketFile(TicketGeneratedMessage ticket) {
        logger.info(TICKET_FILE_GENERATION, "generateTicketFile method entered");

        BufferedImage qrCode;
        try {
            qrCode = FileGenerationUtils.generateQRCodeImage(ticket.getId().toString(), 150, 150);
        } catch (WriterException ex) {
            logger.error(TICKET_FILE_GENERATION, "Error generating QR Code Image: {}", ex.getMessage());
            throw new QrCodeGenerationException("Error generating QR Code Image");
        }

        EventDTO eventDTO;
        EventSessionDTO eventSessionDTO;
        SessionTierDTO tierDTO;

        try {
            eventDTO = eventsClient.getEvent(ticket.getEventId()).getBody();
        } catch (FeignException e) {
            logger.error(TICKET_FILE_GENERATION, "Error getting event: {}", e.getMessage());
            throw new ExternalServiceException("Error getting event");
        }

        eventSessionDTO = eventDTO.getSessions().stream()
                .filter(session -> session.getId().equals(ticket.getSessionId()))
                .findFirst()
                .orElseThrow(() -> new ExternalServiceException("Session not found"));

        tierDTO = eventSessionDTO.getTiers().stream()
                .filter(tier -> tier.getId().equals(ticket.getTierId()))
                .findFirst()
                .orElseThrow(() -> new ExternalServiceException("Tier not found"));

        VenueDTO venueDTO;
        try {
            venueDTO = venuesClient.getVenue(eventSessionDTO.getVenueId()).getBody();
        } catch (FeignException e) {
            logger.error(TICKET_FILE_GENERATION, "Error getting venue: {}", e.getMessage());
            throw new ExternalServiceException("Error getting venue");
        }

        VenueZoneDTO venueZoneDTO;
        try {
            venueZoneDTO = venuesClient.getZone(tierDTO.getZoneId()).getBody();
        } catch (FeignException e) {
            logger.error(TICKET_FILE_GENERATION, "Error getting venue zone: {}", e.getMessage());
            throw new ExternalServiceException("Error getting venue zone");
        }

        TicketInformation information = new TicketInformation();
        information.setEventName(eventDTO.getName());
        information.setVenueName(venueDTO.getName());
        information.setTier(venueZoneDTO.getName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        String formattedDate = eventSessionDTO.getStartsAt() // Instant
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(formatter);

        information.setEventDate(formattedDate);

        BufferedImage logo;
        try (InputStream logoStream = getClass().getResourceAsStream("/" + IMAGES_PATH + "/evently.jpg")) {
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

    /**
     * Generates a ticket PDF and saves it on disk under {@link #TICKET_FILE_PATH}.
     *
     * @param ticket message containing the ticket id and metadata needed for generation
     * @throws FileSaveException         if writing the file fails
     * @throws QrCodeGenerationException if QR code generation fails (propagated)
     * @throws LogoNotFoundException     if logo/template resources are missing (propagated)
     * @throws TemplateNotFoundException if template is missing (propagated)
     * @throws FileGenerationException   if PDF generation fails (propagated)
     */
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

        fileGenerationMessagesPublisher.publishTicketFileGeneratedMessage(ticket);
    }

    /**
     * Loads a ticket PDF from disk.
     *
     * @param ticketId ticket identifier
     * @return PDF bytes
     * @throws TicketFileNotFoundException if the file does not exist or cannot be read
     */
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
