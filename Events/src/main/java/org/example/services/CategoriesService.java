package org.example.services;

import jakarta.transaction.Transactional;
import org.example.enums.EventStatus;
import org.example.exceptions.*;
import org.example.models.Category;
import org.example.models.Event;
import org.example.repositories.CategoriesRepository;
import org.example.repositories.EventsRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriesService {

    @Autowired
    private CategoriesRepository categoriesRepository;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(CategoriesService.class);

    private Marker createMarker = MarkerFactory.getMarker("Category create operation");

    @Transactional
    public Category createCategory(Category category) {
        logger.info(createMarker, "Method create category entered (CategoriesService)");
        if (categoriesRepository.existsByName(category.getName())) {
            logger.error("Category with the name {} already exists (CategoriesService)", category.getName());
            throw new CategoryAlreadyExistsException("Category with name " + category.getName() + " already exists");
        }

        if (category.getName() == null || category.getName().isEmpty()) {
            logger.error("Empty category name (CategoriesService)");
            throw new InvalidCategoryException("Empty category name");
        }

        return categoriesRepository.save(category);
    }

    @Transactional
    public Category updateCategory(UUID id, Category category) {
        if (!id.equals(category.getId())) {
            throw new InvalidCategoryUpdateException("Parameter id and body id do not correspond");
        }

        Category existingCategory = categoriesRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        if (category.getName() == null || category.getName().isEmpty()) {
            throw new InvalidCategoryException("Empty category name");
        }

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(category, existingCategory);

        return categoriesRepository.save(existingCategory);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category existingCategory = categoriesRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        categoriesRepository.delete(existingCategory);
    }

    public Category getCategory(UUID categoryId) {
        return categoriesRepository
                .findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }

    public List<Category> getCategories() {
        return categoriesRepository.findAll();
    }
}
