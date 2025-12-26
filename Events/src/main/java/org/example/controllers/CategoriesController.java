package org.example.controllers;

import org.example.dtos.categories.CategoryCreateDTO;
import org.example.dtos.categories.CategoryDTO;
import org.example.dtos.categories.CategoryUpdateDTO;
import org.example.exceptions.CategoryAlreadyExistsException;
import org.example.exceptions.CategoryNotFoundException;
import org.example.exceptions.InvalidCategoryException;
import org.example.exceptions.InvalidCategoryUpdateException;
import org.example.models.Category;
import org.example.services.CategoriesService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events/categories")
public class CategoriesController {

    @Autowired
    private CategoriesService categoriesService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(CategoriesController.class);

    private Marker marker = MarkerFactory.getMarker("CategoriesController");

    @GetMapping("/get-categories")
    public ResponseEntity<?> getCategories() {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         */
        logger.info(marker, "Method getCategories entered");
        List<CategoryDTO> categories = new ArrayList<>();

        try {
            categories = categoriesService.getCategories()
                    .stream()
                    .map(category -> modelMapper.map(category, CategoryDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while getting categories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, categories found");
        return ResponseEntity.status(HttpStatus.OK).body(categories);
    }

    @GetMapping("/get-category/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Category not found
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method getCategoryById entered");
        CategoryDTO category = null;

        try {
            category = modelMapper.map(categoriesService.getCategory(id), CategoryDTO.class);
        } catch (CategoryNotFoundException e) {
            logger.error(marker, "CategoryNotFoundException caught while getting category");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while getting category");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, category found");
        return ResponseEntity.status(HttpStatus.OK).body(category);
    }

    @PostMapping("/create-category")
    public ResponseEntity<?> createCategory(CategoryCreateDTO createDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid category creation
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method createCategory entered");

        Category categoryToCreate = modelMapper.map(createDTO, Category.class);

        CategoryDTO createdCategory = null;

        try {
            createdCategory = modelMapper.map(categoriesService.createCategory(categoryToCreate), CategoryDTO.class);
        } catch (CategoryAlreadyExistsException e) {
            logger.error(marker, "CategoryAlreadyExistsException caught while creating category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidCategoryException e) {
            logger.error(marker, "InvalidCategoryException caught while getting category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while creating category");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, category created");
        return ResponseEntity.status(HttpStatus.OK).body(createdCategory);
    }

    @GetMapping("/delete-category/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Category not found
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method deleteCategory entered");

        try {
            categoriesService.deleteCategory(id);
        } catch (CategoryNotFoundException e) {
            logger.error(marker, "CategoryNotFoundException caught while deleting category");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while deleting category");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, category deleted");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/update-category/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable("id") UUID id, CategoryUpdateDTO updateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid category creation
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method updateCategory entered");

        Category categoryToUpdate = modelMapper.map(updateDTO, Category.class);

        CategoryDTO updatedCategory = null;

        try {
            updatedCategory = modelMapper.map(categoriesService.updateCategory(id, categoryToUpdate), CategoryDTO.class);
        } catch (InvalidCategoryUpdateException e) {
            logger.error(marker, "InvalidCategoryUpdateException caught while updating category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CategoryNotFoundException e) {
            logger.error(marker, "CategoryNotFoundException caught while updating category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidCategoryException e) {
            logger.error(marker, "InvalidCategoryException caught while updating category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while updating category");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, category updated");
        return ResponseEntity.status(HttpStatus.OK).body(updatedCategory);
    }

}
