package org.example.services;

import org.example.enums.EventStatus;
import org.example.exceptions.*;
import org.example.models.Category;
import org.example.models.Event;
import org.example.repositories.CategoriesRepository;
import org.example.repositories.EventsRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class CategoriesService {

    @Autowired
    private CategoriesRepository categoriesRepository;

    private ModelMapper modelMapper = new ModelMapper();

    @Transactional
    public Category createCategory(Category category) {
        if (categoriesRepository.existsByName(category.getName())) {
            throw new CategoryAlreadyExistsException("Category with name " + category.getName() + " already exists");
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
