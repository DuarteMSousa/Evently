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

    private static final Marker CATEGORIES_GET = MarkerFactory.getMarker("CATEGORIES_GET");
    private static final Marker CATEGORY_GET = MarkerFactory.getMarker("CATEGORY_GET");
    private static final Marker CATEGORY_DELETE = MarkerFactory.getMarker("CATEGORY_DELETE");
    private static final Marker CATEGORY_UPDATE = MarkerFactory.getMarker("CATEGORY_UPDATE");
    private static final Marker CATEGORY_CREATE = MarkerFactory.getMarker("CATEGORY_CREATE");


    @GetMapping("/get-categories")
    public ResponseEntity<?> getCategories() {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         */
        logger.info(CATEGORIES_GET, "Method getCategories entered");
        List<CategoryDTO> categories = new ArrayList<>();

        try {
            categories = categoriesService.getCategories()
                    .stream()
                    .map(category -> modelMapper.map(category, CategoryDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(CATEGORIES_GET, "Exception caught while getting categories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        logger.info(CATEGORIES_GET, "200 OK returned, categories found");
        return ResponseEntity.status(HttpStatus.OK).body(categories);
    }

    @GetMapping("/get-category/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Category not found
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(CATEGORY_GET, "Method getCategoryById entered");
        CategoryDTO category = null;

        try {
            category = modelMapper.map(categoriesService.getCategory(id), CategoryDTO.class);
        } catch (CategoryNotFoundException e) {
            logger.error(CATEGORY_GET, "CategoryNotFoundException caught while getting category");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(CATEGORY_GET, "Exception caught while getting category");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(CATEGORY_GET, "200 OK returned, category found");
        return ResponseEntity.status(HttpStatus.OK).body(category);
    }

    @PostMapping("/create-category")
    public ResponseEntity<?> createCategory(@RequestBody CategoryCreateDTO createDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid category creation
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(CATEGORY_CREATE, "Method createCategory entered");

        Category categoryToCreate = modelMapper.map(createDTO, Category.class);

        CategoryDTO createdCategory = null;

        try {
            createdCategory = modelMapper.map(categoriesService.createCategory(categoryToCreate), CategoryDTO.class);
        } catch (CategoryAlreadyExistsException e) {
            logger.error(CATEGORY_CREATE, "CategoryAlreadyExistsException caught while creating category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidCategoryException e) {
            logger.error(CATEGORY_CREATE, "InvalidCategoryException caught while getting category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(CATEGORY_CREATE, "Exception caught while creating category");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(CATEGORY_CREATE, "200 OK returned, category created");
        return ResponseEntity.status(HttpStatus.OK).body(createdCategory);
    }

    @GetMapping("/delete-category/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Category not found
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(CATEGORY_DELETE, "Method deleteCategory entered");

        try {
            categoriesService.deleteCategory(id);
        } catch (CategoryNotFoundException e) {
            logger.error(CATEGORY_DELETE, "CategoryNotFoundException caught while deleting category");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(CATEGORY_DELETE, "Exception caught while deleting category");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(CATEGORY_DELETE, "200 OK returned, category deleted");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/update-category/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable("id") UUID id, @RequestBody CategoryUpdateDTO updateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid category creation
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(CATEGORY_UPDATE, "Method updateCategory entered");

        Category categoryToUpdate = modelMapper.map(updateDTO, Category.class);

        CategoryDTO updatedCategory = null;

        try {
            updatedCategory = modelMapper.map(categoriesService.updateCategory(id, categoryToUpdate), CategoryDTO.class);
        } catch (InvalidCategoryUpdateException e) {
            logger.error(CATEGORY_UPDATE, "InvalidCategoryUpdateException caught while updating category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CategoryNotFoundException e) {
            logger.error(CATEGORY_UPDATE, "CategoryNotFoundException caught while updating category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidCategoryException e) {
            logger.error(CATEGORY_UPDATE, "InvalidCategoryException caught while updating category");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(CATEGORY_UPDATE, "Exception caught while updating category");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(CATEGORY_UPDATE, "200 OK returned, category updated");
        return ResponseEntity.status(HttpStatus.OK).body(updatedCategory);
    }

}
