package org.evently.services;

import org.evently.dtos.venues.VenueSearchDTO;
import org.evently.exceptions.InvalidVenueException;
import org.evently.exceptions.VenueAlreadyDeactivatedException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.models.Venue;
import org.evently.repositories.VenuesRepository;
import org.evently.service.VenuesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenuesServiceTest {

    @Mock
    private VenuesRepository venuesRepository;

    @InjectMocks
    private VenuesService venuesService;

    private Venue validVenue;

    @BeforeEach
    void setup() {
        validVenue = new Venue();
        validVenue.setId(null);
        validVenue.setName("Altice Arena");
        validVenue.setAddress("Av. Alguma Coisa");
        validVenue.setCity("Lisboa");
        validVenue.setCountry("Portugal");
        validVenue.setPostalCode("1000-000");
        validVenue.setCapacity(1000);
        validVenue.setCreatedBy(UUID.randomUUID());
    }

    // -----------------------
    // createVenue (VCS001-011)
    // -----------------------

    @Test
    void createVenue_capacityNull_throwsInvalidVenueException() {
        validVenue.setCapacity(null);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("Capacity must be greater than 0", ex.getMessage());
        verifyNoInteractions(venuesRepository);
    }

    @Test
    void createVenue_capacityZeroOrNegative_throwsInvalidVenueException() {
        validVenue.setCapacity(0);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("Capacity must be greater than 0", ex.getMessage());
        verifyNoInteractions(venuesRepository);
    }

    @Test
    void createVenue_nameNull_throwsInvalidVenueException() {
        validVenue.setName(null);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("Name is required", ex.getMessage());
        verifyNoInteractions(venuesRepository);
    }

    @Test
    void createVenue_addressNull_throwsInvalidVenueException() {
        validVenue.setAddress(null);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("Address is required", ex.getMessage());
        verifyNoInteractions(venuesRepository);
    }

    @Test
    void createVenue_cityNull_throwsInvalidVenueException() {
        validVenue.setCity(null);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("City is required", ex.getMessage());
        verifyNoInteractions(venuesRepository);
    }

    @Test
    void createVenue_countryNull_throwsInvalidVenueException() {
        validVenue.setCountry(null);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("Country is required", ex.getMessage());
        verifyNoInteractions(venuesRepository);
    }

    @Test
    void createVenue_postalCodeNull_throwsInvalidVenueException() {
        validVenue.setPostalCode(null);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("Postal code is required", ex.getMessage());
        verifyNoInteractions(venuesRepository);
    }

    @Test
    void createVenue_createdByNullOnCreate_throwsInvalidVenueException() {
        validVenue.setCreatedBy(null);
        validVenue.setId(null);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("CreatedBy is required", ex.getMessage());
        verifyNoInteractions(venuesRepository);
    }

    @Test
    void createVenue_duplicateName_throwsInvalidVenueException() {
        when(venuesRepository.existsByName("Altice Arena")).thenReturn(true);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.createVenue(validVenue));

        assertEquals("Venue with name Altice Arena already exists", ex.getMessage());
        verify(venuesRepository).existsByName("Altice Arena");
        verify(venuesRepository, never()).save(any());
    }

    @Test
    void createVenue_valid_setsActiveTrue_andSaves() {
        when(venuesRepository.existsByName("Altice Arena")).thenReturn(false);

        Venue saved = new Venue();
        saved.setId(UUID.randomUUID());
        saved.setName(validVenue.getName());
        saved.setAddress(validVenue.getAddress());
        saved.setCity(validVenue.getCity());
        saved.setCountry(validVenue.getCountry());
        saved.setPostalCode(validVenue.getPostalCode());
        saved.setCapacity(validVenue.getCapacity());
        saved.setCreatedBy(validVenue.getCreatedBy());
        saved.setActive(true);

        when(venuesRepository.save(any(Venue.class))).thenReturn(saved);

        Venue result = venuesService.createVenue(validVenue);

        assertNotNull(result.getId());
        assertTrue(result.isActive());
        assertEquals("Altice Arena", result.getName());

        ArgumentCaptor<Venue> captor = ArgumentCaptor.forClass(Venue.class);
        verify(venuesRepository).save(captor.capture());
        assertTrue(captor.getValue().isActive(), "Service deve marcar active=true antes de guardar");
    }

    @Test
    void createVenue_capacityBoundaryLowerValid_one_succeeds() {
        validVenue.setCapacity(1);
        when(venuesRepository.existsByName(anyString())).thenReturn(false);
        when(venuesRepository.save(any(Venue.class))).thenAnswer(inv -> {
            Venue v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        Venue result = venuesService.createVenue(validVenue);

        assertEquals(1, result.getCapacity());
        assertTrue(result.isActive());
    }

    // -----------------------
    // deactivateVenue (VDS001-003)
    // -----------------------

    @Test
    void deactivateVenue_notFound_throwsVenueNotFoundException() {
        UUID id = UUID.randomUUID();
        when(venuesRepository.findById(id)).thenReturn(Optional.empty());

        VenueNotFoundException ex = assertThrows(VenueNotFoundException.class,
                () -> venuesService.deactivateVenue(id));

        assertEquals("Venue not found", ex.getMessage());
        verify(venuesRepository, never()).save(any());
    }

    @Test
    void deactivateVenue_alreadyDeactivated_throwsVenueAlreadyDeactivatedException() {
        UUID id = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(id);
        venue.setActive(false);

        when(venuesRepository.findById(id)).thenReturn(Optional.of(venue));

        VenueAlreadyDeactivatedException ex = assertThrows(VenueAlreadyDeactivatedException.class,
                () -> venuesService.deactivateVenue(id));

        assertEquals("Venue already deactivated", ex.getMessage());
        verify(venuesRepository, never()).save(any());
    }

    @Test
    void deactivateVenue_success_setsActiveFalse_andSaves() {
        UUID id = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(id);
        venue.setActive(true);

        when(venuesRepository.findById(id)).thenReturn(Optional.of(venue));
        when(venuesRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

        Venue result = venuesService.deactivateVenue(id);

        assertFalse(result.isActive());
        verify(venuesRepository).save(venue);
    }

    // -----------------------
    // getVenue (VGS001-002)
    // -----------------------

    @Test
    void getVenue_notFound_throwsVenueNotFoundException() {
        UUID id = UUID.randomUUID();
        when(venuesRepository.findById(id)).thenReturn(Optional.empty());

        VenueNotFoundException ex = assertThrows(VenueNotFoundException.class,
                () -> venuesService.getVenue(id));

        assertEquals("Venue not found", ex.getMessage());
    }

    @Test
    void getVenue_success_returnsVenue() {
        UUID id = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(id);

        when(venuesRepository.findById(id)).thenReturn(Optional.of(venue));

        Venue result = venuesService.getVenue(id);

        assertEquals(id, result.getId());
    }

    // -----------------------
    // searchVenues (VSS001-009)
    // -----------------------

    @Test
    void searchVenues_minCapacityNegative_throwsInvalidVenueException() {
        VenueSearchDTO dto = new VenueSearchDTO();
        dto.setMinCapacity(-1);

        InvalidVenueException ex = assertThrows(InvalidVenueException.class,
                () -> venuesService.searchVenues(dto));

        assertEquals("minCapacity must be >= 0", ex.getMessage());
        verify(venuesRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void searchVenues_noFilters_callsFindAll_andReturnsList() {
        VenueSearchDTO dto = new VenueSearchDTO();
        when(venuesRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<Venue> res = venuesService.searchVenues(dto);

        assertNotNull(res);
        assertEquals(0, res.size());
        verify(venuesRepository).findAll(any(Specification.class));
    }

    @Test
    void searchVenues_minCapacityBoundaryZero_succeeds() {
        VenueSearchDTO dto = new VenueSearchDTO();
        dto.setMinCapacity(0);

        when(venuesRepository.findAll(any(Specification.class))).thenReturn(List.of(new Venue()));

        List<Venue> res = venuesService.searchVenues(dto);

        assertEquals(1, res.size());
    }
}
