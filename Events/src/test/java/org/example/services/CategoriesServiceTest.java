package org.example.services;

import org.example.exceptions.*;
import org.example.models.Category;
import org.example.repositories.CategoriesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriesServiceTest {

    @Mock
    private CategoriesRepository categoriesRepository;

    @InjectMocks
    private CategoriesService categoriesService;

    @Test
    void createCategory_success_savesAndReturns() {
        Category c = new Category();
        c.setName("Music");

        when(categoriesRepository.existsByName("Music")).thenReturn(false);
        when(categoriesRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category res = categoriesService.createCategory(c);

        assertEquals("Music", res.getName());
        verify(categoriesRepository).save(c);
    }

    @Test
    void createCategory_nameNull_throwsInvalidCategoryException() {
        Category c = new Category();
        c.setName(null);

        when(categoriesRepository.existsByName(null)).thenReturn(false);

        InvalidCategoryException ex = assertThrows(InvalidCategoryException.class,
                () -> categoriesService.createCategory(c));

        assertEquals("Empty category name", ex.getMessage());
        verify(categoriesRepository, never()).save(any());
    }

    @Test
    void createCategory_alreadyExists_throwsCategoryAlreadyExistsException() {
        Category c = new Category();
        c.setName("Music");

        when(categoriesRepository.existsByName("Music")).thenReturn(true);

        CategoryAlreadyExistsException ex = assertThrows(CategoryAlreadyExistsException.class,
                () -> categoriesService.createCategory(c));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(categoriesRepository, never()).save(any());
    }

    @Test
    void updateCategory_idMismatch_throwsInvalidCategoryUpdateException() {
        UUID pathId = UUID.randomUUID();
        Category payload = new Category();
        payload.setId(UUID.randomUUID());
        payload.setName("New");

        InvalidCategoryUpdateException ex = assertThrows(InvalidCategoryUpdateException.class,
                () -> categoriesService.updateCategory(pathId, payload));

        assertEquals("Parameter id and body id do not correspond", ex.getMessage());
        verify(categoriesRepository, never()).save(any());
    }

    @Test
    void updateCategory_notFound_throwsCategoryNotFoundException() {
        UUID id = UUID.randomUUID();
        Category payload = new Category();
        payload.setId(id);
        payload.setName("New");

        when(categoriesRepository.findById(id)).thenReturn(Optional.empty());

        CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class,
                () -> categoriesService.updateCategory(id, payload));

        assertEquals("Category not found", ex.getMessage());
        verify(categoriesRepository, never()).save(any());
    }

    @Test
    void updateCategory_emptyName_throwsInvalidCategoryException() {
        UUID id = UUID.randomUUID();
        Category payload = new Category();
        payload.setId(id);
        payload.setName("");

        Category existing = new Category();
        existing.setId(id);
        existing.setName("Old");

        when(categoriesRepository.findById(id)).thenReturn(Optional.of(existing));

        InvalidCategoryException ex = assertThrows(InvalidCategoryException.class,
                () -> categoriesService.updateCategory(id, payload));

        assertEquals("Empty category name", ex.getMessage());
        verify(categoriesRepository, never()).save(any());
    }

    @Test
    void updateCategory_success_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        Category payload = new Category();
        payload.setId(id);
        payload.setName("NewName");

        Category existing = new Category();
        existing.setId(id);
        existing.setName("OldName");

        when(categoriesRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoriesRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category res = categoriesService.updateCategory(id, payload);

        assertEquals("NewName", res.getName());
        verify(categoriesRepository).save(existing);
    }

    @Test
    void deleteCategory_notFound_throwsCategoryNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoriesRepository.findById(id)).thenReturn(Optional.empty());

        CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class,
                () -> categoriesService.deleteCategory(id));

        assertEquals("Category not found", ex.getMessage());
        verify(categoriesRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_success_deletes() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);

        when(categoriesRepository.findById(id)).thenReturn(Optional.of(existing));

        categoriesService.deleteCategory(id);

        verify(categoriesRepository).delete(existing);
    }

    @Test
    void getCategory_notFound_throwsCategoryNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoriesRepository.findById(id)).thenReturn(Optional.empty());

        CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class,
                () -> categoriesService.getCategory(id));

        assertEquals("Category not found", ex.getMessage());
    }

    @Test
    void getCategory_success_returns() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("X");

        when(categoriesRepository.findById(id)).thenReturn(Optional.of(existing));

        Category res = categoriesService.getCategory(id);

        assertEquals(id, res.getId());
    }

    @Test
    void getCategories_success_returnsList() {
        when(categoriesRepository.findAll()).thenReturn(Arrays.asList(new Category(), new Category()));

        List<Category> res = categoriesService.getCategories();

        assertEquals(2, res.size());
        verify(categoriesRepository).findAll();
    }
}
