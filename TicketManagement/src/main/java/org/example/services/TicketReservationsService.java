package org.example.services;

import org.example.models.TicketReservation;
import org.example.repositories.CategoriesRepository;
import org.example.repositories.TicketReservationsRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class TicketReservationsService {

    @Autowired
    private TicketReservationsRepository ticketReservationsRepository;

    private ModelMapper modelMapper = new ModelMapper();

    @Transactional
    public TicketReservation createTicketReservation() {


        return ticketReservationsRepository.save(ticketReservation);
    }

    @Transactional
    public TicketReservation updateCategory(UUID id, TicketReservation category) {
        if (!id.equals(category.getId())) {
            throw new InvalidCategoryUpdateException("Parameter id and body id do not correspond");
        }

        TicketReservation existingCategory = categoriesRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(category, existingCategory);

        return categoriesRepository.save(existingCategory);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        TicketReservation existingCategory = categoriesRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        categoriesRepository.delete(existingCategory);
    }

    public TicketReservation getCategory(UUID categoryId) {
        return categoriesRepository
                .findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }

    public List<TicketReservation> getCategories() {
        return categoriesRepository.findAll();
    }
}
