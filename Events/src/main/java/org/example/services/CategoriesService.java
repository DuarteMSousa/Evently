package org.example.services;

import jakarta.transaction.Transactional;
import org.example.exceptions.*;
import org.example.models.Category;
import org.example.repositories.CategoriesRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriesService {

    @Autowired
    private CategoriesRepository categoriesRepository;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(CategoriesService.class);

    private static final Marker CATEGORIES_GET = MarkerFactory.getMarker("CATEGORIES_GET");
    private static final Marker CATEGORY_GET = MarkerFactory.getMarker("CATEGORY_GET");
    private static final Marker CATEGORY_DELETE = MarkerFactory.getMarker("CATEGORY_DELETE");
    private static final Marker CATEGORY_UPDATE = MarkerFactory.getMarker("CATEGORY_UPDATE");
    private static final Marker CATEGORY_CREATE = MarkerFactory.getMarker("CATEGORY_CREATE");


    @Transactional
    public Category createCategory(Category category) {
        logger.info(CATEGORY_CREATE, "Method create category entered");
        if (categoriesRepository.existsByName(category.getName())) {
            logger.error(CATEGORY_CREATE, "Category with the name {} already exists", category.getName());
            throw new CategoryAlreadyExistsException("Category with name " + category.getName() + " already exists");
        }

        if (category.getName() == null || category.getName().isEmpty()) {
            logger.error(CATEGORY_CREATE, "Empty category name");
            throw new InvalidCategoryException("Empty category name");
        }

        return categoriesRepository.save(category);
    }

    @Transactional
    public Category updateCategory(UUID id, Category category) {
        logger.info(CATEGORY_UPDATE, "Method updateCategory entered");
        if (!id.equals(category.getId())) {
            logger.error(CATEGORY_UPDATE, "Parameter id and body id do not correspond");
            throw new InvalidCategoryUpdateException("Parameter id and body id do not correspond");
        }

        Category existingCategory = categoriesRepository.findById(id)
                .orElse(null);

        if (existingCategory == null) {
            logger.error(CATEGORY_UPDATE, "Category not found");
            throw new CategoryNotFoundException("Category not found");
        }

        if (category.getName() == null || category.getName().isEmpty()) {
            logger.error(CATEGORY_UPDATE, "Empty category name");
            throw new InvalidCategoryException("Empty category name");
        }

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(category, existingCategory);

        return categoriesRepository.save(existingCategory);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        logger.info(CATEGORY_DELETE, "Method deleteCategory entered");
        Category existingCategory = categoriesRepository.findById(id)
                .orElse(null);

        if (existingCategory == null) {
            logger.error(CATEGORY_DELETE, "Category not found");
            throw new CategoryNotFoundException("Category not found");
        }

        categoriesRepository.delete(existingCategory);
    }

    public Category getCategory(UUID categoryId) {
        logger.info(CATEGORY_GET, "Method getCategory entered");
        Category category = categoriesRepository
                .findById(categoryId)
                .orElse(null);

        if (category == null) {
            logger.error(CATEGORY_GET, "Category not found");
            throw new CategoryNotFoundException("Category not found");
        }

        return category;
    }

    public List<Category> getCategories() {
        logger.info(CATEGORIES_GET, "Method getCategories entered");
        return categoriesRepository.findAll();
    }
}
