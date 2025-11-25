package org.example.services;

import org.example.models.Cart;
import org.example.repositories.CategoriesRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Cart createCategory(Cart category) {
        if (categoriesRepository.existsByName(category.getName())) {
            throw new CategoryAlreadyExistsException("Category with name " + category.getName() + " already exists");
        }

        return categoriesRepository.save(category);
    }

    @Transactional
    public Cart updateCategory(UUID id, Cart category) {
        if (!id.equals(category.getId())) {
            throw new InvalidCategoryUpdateException("Parameter id and body id do not correspond");
        }

        Cart existingCategory = categoriesRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(category, existingCategory);

        return categoriesRepository.save(existingCategory);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Cart existingCategory = categoriesRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        categoriesRepository.delete(existingCategory);
    }

    public Cart getCategory(UUID categoryId) {
        return categoriesRepository
                .findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }

    public List<Cart> getCategories() {
        return categoriesRepository.findAll();
    }
}
