package org.example.controllers;

import org.example.dtos.categories.CategoryCreateDTO;
import org.example.dtos.categories.CategoryDTO;
import org.example.dtos.categories.CategoryUpdateDTO;
import org.example.dtos.events.EventCreateDTO;
import org.example.dtos.events.EventDTO;
import org.example.dtos.events.EventUpdateDTO;
import org.example.exceptions.*;
import org.example.models.Category;
import org.example.models.Event;
import org.example.services.CategoriesService;
import org.example.services.EventsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events")
public class EventsController {


    @Autowired
    private EventsService eventsService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(EventsController.class);

    private Marker marker = MarkerFactory.getMarker("EventsController");

    @GetMapping("/get-events-page/{pageNumber}/{pageSize}")
    public ResponseEntity<?> searchEvents(@PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 500 INTERNAL_SERVER_ERROR - undefined error
         */
        logger.info(marker, "Method searchEvents entered");
        Page<EventDTO> eventsPage;

        try {
            eventsPage = eventsService.getEventPage(pageNumber, pageSize).map(event -> modelMapper.map(event, EventDTO.class));
        } catch (Exception e) {
            logger.error(marker, "Exception caught while getting events: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, events found");
        return ResponseEntity.status(HttpStatus.OK).body(eventsPage);
    }

    @PostMapping("/create-event/")
    public ResponseEntity<?> createEvent(EventCreateDTO createDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event creation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method createEvent entered");
        Event eventToCreate = modelMapper.map(createDTO, Event.class);
        EventDTO event = null;

        try {
            event = modelMapper.map(eventsService.createEvent(eventToCreate), EventDTO.class);
        } catch (EventAlreadyExistsException e) {
            logger.error(marker, "EventAlreadyExistsException caught while creating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventException e) {
            logger.error(marker, "InvalidEventException caught while creating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while creating Event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, event created");
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    @PutMapping("/update-event/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable("id") UUID id,EventUpdateDTO updateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event creation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method updateEvent entered");
        Event eventToUpdate = modelMapper.map(updateDTO, Event.class);
        EventDTO event = null;

        try {
            event = modelMapper.map(eventsService.updateEvent(id,eventToUpdate), EventDTO.class);
        } catch (InvalidEventUpdateException e) {
            logger.error(marker, "InvalidEventUpdateException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (EventAlreadyExistsException e) {
            logger.error(marker, "EventAlreadyExistsException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventException e) {
            logger.error(marker, "InvalidEventException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while updating Event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, event updated");
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

//    @GetMapping("/delete-category/{id}")
//    public ResponseEntity<?> deleteCategory(@PathVariable("id") UUID id) {
//        /* HttpStatus(produces)
//         * 200 OK - Request processed as expected.
//         * 404 NOT_FOUND - Category not found
//         * 500 INTERNAL_SERVER_ERROR - Internal server error.
//         */
//        logger.info(marker, "Method deleteCategory entered");
//
//        try {
//            categoriesService.deleteCategory(id);
//        } catch (CategoryNotFoundException e) {
//            logger.error(marker, "CategoryNotFoundException caught while deleting category");
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//        } catch (Exception e) {
//            logger.error(marker, "Exception caught while deleting category");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
//        }
//
//        logger.info(marker, "200 OK returned, category deleted");
//        return ResponseEntity.status(HttpStatus.OK).build();
//    }
//
//    @PostMapping("/update-category/{id}")
//    public ResponseEntity<?> updateCategory(@PathVariable("id") UUID id, CategoryUpdateDTO updateDTO) {
//        /* HttpStatus(produces)
//         * 200 OK - Request processed as expected.
//         * 400 BAD_REQUEST - Invalid category creation
//         * 500 INTERNAL_SERVER_ERROR - Internal server error.
//         */
//        logger.info(marker, "Method updateCategory entered");
//
//        Category categoryToUpdate = modelMapper.map(updateDTO, Category.class);
//
//        CategoryDTO updatedCategory = null;
//
//        try {
//            updatedCategory = modelMapper.map(categoriesService.updateCategory(id, categoryToUpdate), CategoryDTO.class);
//        } catch (InvalidCategoryUpdateException e) {
//            logger.error(marker, "InvalidCategoryUpdateException caught while updating category");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        } catch (CategoryNotFoundException e) {
//            logger.error(marker, "CategoryNotFoundException caught while updating category");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        } catch (InvalidCategoryException e) {
//            logger.error(marker, "InvalidCategoryException caught while updating category");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        } catch (Exception e) {
//            logger.error(marker, "Exception caught while updating category");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
//        }
//
//        logger.info(marker, "200 OK returned, category updated");
//        return ResponseEntity.status(HttpStatus.OK).body(updatedCategory);
//    }
}
