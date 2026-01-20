package org.evently.services;

import org.evently.exceptions.InvalidVenueZoneException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.exceptions.VenueZoneNotFoundException;
import org.evently.models.Venue;
import org.evently.models.VenueZone;
import org.evently.repositories.VenueZoneRepository;
import org.evently.repositories.VenuesRepository;
import org.evently.service.VenueZonesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueZonesServiceTest {

    @Mock
    private VenueZoneRepository venueZoneRepository;

    @Mock
    private VenuesRepository venuesRepository;

    @InjectMocks
    private VenueZonesService venueZonesService;

    private Venue venue;
    private VenueZone validZone;
    private UUID venueId;

    @BeforeEach
    void setup() {
        venueId = UUID.randomUUID();

        venue = new Venue();
        venue.setId(venueId);
        venue.setCapacity(100);

        validZone = new VenueZone();
        validZone.setId(null);
        validZone.setName("Zona A");
        validZone.setCapacity(50);
        validZone.setCreatedBy(UUID.randomUUID());
    }

    // createVenueZone

    @Test
    void createVenueZone_venueNotFound_throwsVenueNotFoundException() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.empty());

        VenueNotFoundException ex = assertThrows(VenueNotFoundException.class,
                () -> venueZonesService.createVenueZone(venueId, validZone));

        assertEquals("Venue not found", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void createVenueZone_nameNull_throwsInvalidVenueZoneException() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.of(venue));
        validZone.setName(null);

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.createVenueZone(venueId, validZone));

        assertEquals("Zone name is required", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void createVenueZone_capacityNull_throwsInvalidVenueZoneException() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.of(venue));
        validZone.setCapacity(null);

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.createVenueZone(venueId, validZone));

        assertEquals("Zone capacity must be greater than 0", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void createVenueZone_capacityZeroOrNegative_throwsInvalidVenueZoneException() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.of(venue));
        validZone.setCapacity(0);

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.createVenueZone(venueId, validZone));

        assertEquals("Zone capacity must be greater than 0", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void createVenueZone_capacityGreaterThanVenue_throwsInvalidVenueZoneException() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.of(venue));
        validZone.setCapacity(101);

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.createVenueZone(venueId, validZone));

        assertEquals("Zone capacity must be less or equal than venue capacity", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void createVenueZone_createdByNullOnCreate_throwsInvalidVenueZoneException() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.of(venue));
        validZone.setCreatedBy(null);
        validZone.setId(null);

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.createVenueZone(venueId, validZone));

        assertEquals("CreatedBy is required", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void createVenueZone_valid_associatesVenue_andSaves() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueZoneRepository.save(any(VenueZone.class))).thenAnswer(inv -> {
            VenueZone z = inv.getArgument(0);
            z.setId(UUID.randomUUID());
            return z;
        });

        VenueZone saved = venueZonesService.createVenueZone(venueId, validZone);

        assertNotNull(saved.getId());
        assertNotNull(saved.getVenue());
        assertEquals(venueId, saved.getVenue().getId());
        verify(venueZoneRepository).save(any(VenueZone.class));
    }

    @Test
    void createVenueZone_capacityBoundaryLowerValid_one_succeeds() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.of(venue));
        validZone.setCapacity(1);

        when(venueZoneRepository.save(any(VenueZone.class))).thenAnswer(inv -> inv.getArgument(0));

        VenueZone saved = venueZonesService.createVenueZone(venueId, validZone);
        assertEquals(1, saved.getCapacity());
    }

    @Test
    void createVenueZone_capacityEqualVenueCapacity_succeeds() {
        when(venuesRepository.findById(venueId)).thenReturn(Optional.of(venue));
        validZone.setCapacity(100);

        when(venueZoneRepository.save(any(VenueZone.class))).thenAnswer(inv -> inv.getArgument(0));

        VenueZone saved = venueZonesService.createVenueZone(venueId, validZone);
        assertEquals(100, saved.getCapacity());
    }

    // getVenueZone

    @Test
    void getVenueZone_notFound_throwsVenueZoneNotFoundException() {
        UUID zoneId = UUID.randomUUID();
        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.empty());

        VenueZoneNotFoundException ex = assertThrows(VenueZoneNotFoundException.class,
                () -> venueZonesService.getVenueZone(zoneId));

        assertEquals("Venue zone not found", ex.getMessage());
    }

    @Test
    void getVenueZone_success_returnsZone() {
        UUID zoneId = UUID.randomUUID();
        VenueZone zone = new VenueZone();
        zone.setId(zoneId);

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));

        VenueZone result = venueZonesService.getVenueZone(zoneId);
        assertEquals(zoneId, result.getId());
    }

    // getVenueZonesByVenue (ZLS001-003)

    @Test
    void getVenueZonesByVenue_venueNotExists_throwsVenueNotFoundException() {
        when(venuesRepository.existsById(venueId)).thenReturn(false);

        VenueNotFoundException ex = assertThrows(VenueNotFoundException.class,
                () -> venueZonesService.getVenueZonesByVenue(venueId));

        assertEquals("Venue not found", ex.getMessage());
        verify(venueZoneRepository, never()).findByVenueId(any());
    }

    @Test
    void getVenueZonesByVenue_exists_noZones_returnsEmptyList() {
        when(venuesRepository.existsById(venueId)).thenReturn(true);
        when(venueZoneRepository.findByVenueId(venueId)).thenReturn(List.of());

        List<VenueZone> res = venueZonesService.getVenueZonesByVenue(venueId);

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    void getVenueZonesByVenue_exists_withZones_returnsList() {
        when(venuesRepository.existsById(venueId)).thenReturn(true);
        when(venueZoneRepository.findByVenueId(venueId)).thenReturn(List.of(new VenueZone(), new VenueZone()));

        List<VenueZone> res = venueZonesService.getVenueZonesByVenue(venueId);

        assertEquals(2, res.size());
    }

    // updateVenueZone

    @Test
    void updateVenueZone_pathIdNotEqualBodyId_throwsInvalidVenueZoneException() {
        UUID pathId = UUID.randomUUID();
        UUID bodyId = UUID.randomUUID();

        VenueZone payload = new VenueZone();
        payload.setId(bodyId);

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.updateVenueZone(pathId, payload));

        assertEquals("Parameter id and body id do not correspond", ex.getMessage());
        verify(venueZoneRepository, never()).findById(any());
    }

    @Test
    void updateVenueZone_zoneNotFound_throwsVenueZoneNotFoundException() {
        UUID zoneId = UUID.randomUUID();
        VenueZone payload = new VenueZone();
        payload.setUpdatedBy(UUID.randomUUID());

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.empty());

        VenueZoneNotFoundException ex = assertThrows(VenueZoneNotFoundException.class,
                () -> venueZonesService.updateVenueZone(zoneId, payload));

        assertEquals("Venue zone not found", ex.getMessage());
    }

    @Test
    void updateVenueZone_updatedByNull_throwsInvalidVenueZoneException() {
        UUID zoneId = UUID.randomUUID();

        VenueZone existing = new VenueZone();
        existing.setId(zoneId);
        existing.setName("Antiga");
        existing.setCapacity(10);
        existing.setCreatedBy(UUID.randomUUID());
        existing.setVenue(venue);

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.of(existing));

        VenueZone payload = new VenueZone(); // updatedBy null

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.updateVenueZone(zoneId, payload));

        assertEquals("UpdatedBy is required", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void updateVenueZone_updateOnlyName_success() {
        UUID zoneId = UUID.randomUUID();

        VenueZone existing = new VenueZone();
        existing.setId(zoneId);
        existing.setName("Antiga");
        existing.setCapacity(10);
        existing.setCreatedBy(UUID.randomUUID());
        existing.setVenue(venue);

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.of(existing));
        when(venueZoneRepository.save(any(VenueZone.class))).thenAnswer(inv -> inv.getArgument(0));

        VenueZone payload = new VenueZone();
        payload.setName("Nova");

        UUID updater = UUID.randomUUID();
        payload.setUpdatedBy(updater);

        VenueZone res = venueZonesService.updateVenueZone(zoneId, payload);

        assertEquals("Nova", res.getName());
        assertEquals(10, res.getCapacity());
        assertEquals(updater, res.getUpdatedBy());
        verify(venueZoneRepository).save(existing);
    }


    @Test
    void updateVenueZone_invalidCapacity_throwsInvalidVenueZoneException() {
        UUID zoneId = UUID.randomUUID();

        VenueZone existing = new VenueZone();
        existing.setId(zoneId);
        existing.setName("Zona");
        existing.setCapacity(10);
        existing.setCreatedBy(UUID.randomUUID());
        existing.setVenue(venue);

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.of(existing));

        VenueZone payload = new VenueZone();
        payload.setCapacity(0);
        payload.setUpdatedBy(UUID.randomUUID());

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.updateVenueZone(zoneId, payload));

        assertEquals("Zone capacity must be greater than 0", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void updateVenueZone_capacityGreaterThanVenue_throwsInvalidVenueZoneException() {
        UUID zoneId = UUID.randomUUID();

        VenueZone existing = new VenueZone();
        existing.setId(zoneId);
        existing.setName("Zona");
        existing.setCapacity(10);
        existing.setCreatedBy(UUID.randomUUID());
        existing.setVenue(venue);

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.of(existing));

        VenueZone payload = new VenueZone();
        payload.setCapacity(101);
        payload.setUpdatedBy(UUID.randomUUID());

        InvalidVenueZoneException ex = assertThrows(InvalidVenueZoneException.class,
                () -> venueZonesService.updateVenueZone(zoneId, payload));

        assertEquals("Zone capacity must be less or equal than venue capacity", ex.getMessage());
        verify(venueZoneRepository, never()).save(any());
    }

    @Test
    void updateVenueZone_validCapacity_success() {
        UUID zoneId = UUID.randomUUID();

        VenueZone existing = new VenueZone();
        existing.setId(zoneId);
        existing.setName("Zona");
        existing.setCapacity(10);
        existing.setCreatedBy(UUID.randomUUID());
        existing.setVenue(venue);

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.of(existing));
        when(venueZoneRepository.save(any(VenueZone.class))).thenAnswer(inv -> inv.getArgument(0));

        VenueZone payload = new VenueZone();
        payload.setCapacity(20);

        UUID updater = UUID.randomUUID();
        payload.setUpdatedBy(updater);

        VenueZone res = venueZonesService.updateVenueZone(zoneId, payload);

        assertEquals(20, res.getCapacity());
        assertEquals(updater, res.getUpdatedBy());
        verify(venueZoneRepository).save(existing);
    }

    @Test
    void updateVenueZone_capacityBoundaryLowerValid_one_succeeds() {
        UUID zoneId = UUID.randomUUID();

        VenueZone existing = new VenueZone();
        existing.setId(zoneId);
        existing.setName("Zona");
        existing.setCapacity(10);
        existing.setCreatedBy(UUID.randomUUID());
        existing.setVenue(venue);

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.of(existing));
        when(venueZoneRepository.save(any(VenueZone.class))).thenAnswer(inv -> inv.getArgument(0));

        VenueZone payload = new VenueZone();
        payload.setCapacity(1);
        payload.setUpdatedBy(UUID.randomUUID());

        VenueZone res = venueZonesService.updateVenueZone(zoneId, payload);

        assertEquals(1, res.getCapacity());
    }

    @Test
    void updateVenueZone_capacityEqualVenueCapacity_succeeds() {
        UUID zoneId = UUID.randomUUID();

        VenueZone existing = new VenueZone();
        existing.setId(zoneId);
        existing.setName("Zona");
        existing.setCapacity(10);
        existing.setCreatedBy(UUID.randomUUID());
        existing.setVenue(venue);

        when(venueZoneRepository.findById(zoneId)).thenReturn(Optional.of(existing));
        when(venueZoneRepository.save(any(VenueZone.class))).thenAnswer(inv -> inv.getArgument(0));

        VenueZone payload = new VenueZone();
        payload.setCapacity(100);
        payload.setUpdatedBy(UUID.randomUUID());

        VenueZone res = venueZonesService.updateVenueZone(zoneId, payload);

        assertEquals(100, res.getCapacity());
    }

}
