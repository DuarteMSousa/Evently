package org.example.services;

import com.google.zxing.WriterException;
import feign.FeignException;
import org.example.clients.EventsClient;
import org.example.clients.VenuesClient;
import org.example.dtos.*;
import org.example.exceptions.*;
import org.example.messages.TicketGeneratedMessage;
import org.example.publishers.FileGenerationMessagesPublisher;
import org.example.utils.FileGenerationUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketFileGenerationServiceTest {

    @Mock
    RabbitTemplate template;
    @Mock FileGenerationMessagesPublisher fileGenerationMessagesPublisher;
    @Mock EventsClient eventsClient;
    @Mock VenuesClient venuesClient;

    @InjectMocks TicketFileGenerationService service;

    @TempDir File tempDir;

    private UUID ticketId;
    private UUID eventId;
    private UUID sessionId;
    private UUID tierId;
    private UUID zoneId;
    private UUID venueId;

    @BeforeEach
    void setup() {
        ticketId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        tierId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        venueId = UUID.randomUUID();

        ReflectionTestUtils.setField(service, "TICKET_FILE_PATH", tempDir.getAbsolutePath());
    }

    private TicketGeneratedMessage baseTicketMessage() {
        TicketGeneratedMessage msg = new TicketGeneratedMessage();
        msg.setId(ticketId);
        msg.setEventId(eventId);
        msg.setSessionId(sessionId);
        msg.setTierId(tierId);
        return msg;
    }

    private EventDTO baseEventDTOGraph() {
        // Tier
        SessionTierDTO tier = new SessionTierDTO();
        tier.setId(tierId);
        tier.setZoneId(zoneId);

        // Session
        EventSessionDTO session = new EventSessionDTO();
        session.setId(sessionId);
        session.setVenueId(venueId);
        session.setStartsAt(Instant.now());
        session.setTiers(List.of(tier));

        // Event
        EventDTO event = new EventDTO();
        event.setId(eventId);
        event.setName("Evento X");
        event.setSessions(List.of(session));
        return event;
    }

    private VenueDTO baseVenueDTO() {
        VenueDTO v = new VenueDTO();
        v.setId(venueId);
        v.setName("Venue Y");
        return v;
    }

    private VenueZoneDTO baseZoneDTO() {
        VenueZoneDTO z = new VenueZoneDTO();
        z.setId(zoneId);
        z.setVenueId(venueId);
        z.setName("Zona Z");
        return z;
    }

    // --------------------
    // TFG_GEN001 (QR error)
    // --------------------
    @Test
    void saveTicketFile_qrGenerationThrowsWriterException_throwsQrCodeGenerationException() throws Exception {
        TicketGeneratedMessage msg = baseTicketMessage();

        try (MockedStatic<FileGenerationUtils> mocked = Mockito.mockStatic(FileGenerationUtils.class)) {
            mocked.when(() -> FileGenerationUtils.generateQRCodeImage(anyString(), anyInt(), anyInt()))
                    .thenThrow(new WriterException("boom"));

            assertThrows(QrCodeGenerationException.class, () -> service.saveTicketFile(msg));

            verify(fileGenerationMessagesPublisher, never()).publishTicketFileGeneratedMessage(any());
        }
    }

    // -----------------------
    // TFG_GEN004 (PDF error)
    // -----------------------
    @Test
    void saveTicketFile_generateTicketPdfThrows_throwsFileGenerationException() {
        TicketGeneratedMessage msg = baseTicketMessage();

        // Precisa passar a fase de QR + calls externas + resources (logo/template).
        // QR ok + generateTicketPdf falha
        try (MockedStatic<FileGenerationUtils> mocked = Mockito.mockStatic(FileGenerationUtils.class)) {
            mocked.when(() -> FileGenerationUtils.generateQRCodeImage(anyString(), anyInt(), anyInt()))
                    .thenReturn(new java.awt.image.BufferedImage(150, 150, java.awt.image.BufferedImage.TYPE_INT_RGB));

            mocked.when(() -> FileGenerationUtils.generateTicketPdf(anyString(), any(), any(), any()))
                    .thenThrow(new RuntimeException("pdf fail"));

            when(eventsClient.getEvent(eventId)).thenReturn(ResponseEntity.ok(baseEventDTOGraph()));
            when(venuesClient.getVenue(venueId)).thenReturn(ResponseEntity.ok(baseVenueDTO()));
            when(venuesClient.getZone(zoneId)).thenReturn(ResponseEntity.ok(baseZoneDTO()));

            assertThrows(FileGenerationException.class, () -> service.saveTicketFile(msg));

            verify(fileGenerationMessagesPublisher, never()).publishTicketFileGeneratedMessage(any());
        }
    }

    // --------------------
    // TFG_SAVE001 success
    // --------------------
    @Test
    void saveTicketFile_success_createsFileAndPublishesMessage() {
        TicketGeneratedMessage msg = baseTicketMessage();
        UUID ticketId = msg.getId();

        byte[] fakePdf = "PDF".getBytes();

        try (MockedStatic<FileGenerationUtils> mocked = Mockito.mockStatic(FileGenerationUtils.class)) {
            mocked.when(() -> FileGenerationUtils.generateQRCodeImage(anyString(), anyInt(), anyInt()))
                    .thenReturn(new java.awt.image.BufferedImage(150, 150, java.awt.image.BufferedImage.TYPE_INT_RGB));

            mocked.when(() -> FileGenerationUtils.generateTicketPdf(anyString(), any(), any(), any()))
                    .thenReturn(fakePdf);

            when(eventsClient.getEvent(any())).thenReturn(ResponseEntity.ok(baseEventDTOGraph()));
            when(venuesClient.getVenue(any())).thenReturn(ResponseEntity.ok(baseVenueDTO()));
            when(venuesClient.getZone(any())).thenReturn(ResponseEntity.ok(baseZoneDTO()));

            service.saveTicketFile(msg);

            File expected = new File("TicketFiles", ticketId + ".pdf");
            assertTrue(expected.exists(), "PDF deve existir no disco");
            assertTrue(expected.length() > 0, "PDF deve ter conteúdo");

            verify(fileGenerationMessagesPublisher).publishTicketFileGeneratedMessage(msg);

            // limpeza opcional
            expected.delete();
        }
    }


    // ----------------------------
    // TFG_SAVE002 (IOException write)
    // ----------------------------
    @Test
    void saveTicketFile_whenFileOutputStreamThrowsIOException_throwsFileSaveException() throws IOException {
        TicketGeneratedMessage msg = baseTicketMessage();

        byte[] fakePdf = "PDF".getBytes();

        try (MockedStatic<FileGenerationUtils> mocked = Mockito.mockStatic(FileGenerationUtils.class);
             MockedConstruction<FileOutputStream> fosMock = Mockito.mockConstruction(FileOutputStream.class,
                     (mock, context) -> doThrow(new IOException("disk full")).when(mock).write(any(byte[].class)))) {

            mocked.when(() -> FileGenerationUtils.generateQRCodeImage(anyString(), anyInt(), anyInt()))
                    .thenReturn(new java.awt.image.BufferedImage(150, 150, java.awt.image.BufferedImage.TYPE_INT_RGB));

            mocked.when(() -> FileGenerationUtils.generateTicketPdf(anyString(), any(), any(), any()))
                    .thenReturn(fakePdf);

            when(eventsClient.getEvent(eventId)).thenReturn(ResponseEntity.ok(baseEventDTOGraph()));
            when(venuesClient.getVenue(venueId)).thenReturn(ResponseEntity.ok(baseVenueDTO()));
            when(venuesClient.getZone(zoneId)).thenReturn(ResponseEntity.ok(baseZoneDTO()));

            assertThrows(FileSaveException.class, () -> service.saveTicketFile(msg));
            verify(fileGenerationMessagesPublisher, never()).publishTicketFileGeneratedMessage(any());
        }
    }

    // --------------------
    // TFG_GET001 not found
    // --------------------
    @Test
    void getTicketPdf_fileDoesNotExist_throwsTicketFileNotFoundException() {
        assertThrows(TicketFileNotFoundException.class, () -> service.getTicketPdf(UUID.randomUUID()));
    }

    // --------------------
    // TFG_GET003 success
    // --------------------
    @Test
    void getTicketPdf_success_returnsBytes() throws Exception {
        UUID id = UUID.randomUUID();

        File dir = new File("TicketFiles");
        dir.mkdirs();

        File f = new File(dir, id + ".pdf");
        Files.write(f.toPath(), "HELLO".getBytes());

        byte[] res = service.getTicketPdf(id);

        assertArrayEquals("HELLO".getBytes(), res);
    }

    // --------------------
    // Extra: external errors
    // --------------------
    @Test
    void saveTicketFile_eventsClientFeign_throwsExternalServiceException() throws Exception {
        TicketGeneratedMessage msg = baseTicketMessage();

        try (MockedStatic<FileGenerationUtils> mocked = Mockito.mockStatic(FileGenerationUtils.class)) {
            mocked.when(() -> FileGenerationUtils.generateQRCodeImage(anyString(), anyInt(), anyInt()))
                    .thenReturn(new java.awt.image.BufferedImage(150, 150, java.awt.image.BufferedImage.TYPE_INT_RGB));

            when(eventsClient.getEvent(eventId)).thenThrow(mock(FeignException.class));

            assertThrows(ExternalServiceException.class, () -> service.saveTicketFile(msg));
        }
    }
}