package org.example.services;

import jakarta.transaction.Transactional;
import org.example.exceptions.EventSessionNotFoundException;
import org.example.exceptions.InvalidSessionTierUpdateException;
import org.example.exceptions.SessionTierAlreadyExistsException;
import org.example.exceptions.SessionTierNotFoundException;
import org.example.models.EventSession;
import org.example.models.SessionTier;
import org.example.repositories.SessionTiersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SessionTiersService {

    @Autowired
    private SessionTiersRepository sessionTiersRepository;

    @Transactional
    public SessionTier createSessionTier(SessionTier sessionTier) {
        if (sessionTiersRepository.existsByEventSessionAndZoneId(sessionTier.getEventSession(), sessionTier.getZoneId())) {
            throw new SessionTierAlreadyExistsException("Session tier already exists");
        }

        //createdby para ver organization

        return sessionTiersRepository.save(sessionTier);
    }

    @Transactional
    public SessionTier updateSessionTier(UUID id, SessionTier sessionTier) {
        if (!id.equals(sessionTier.getId())) {
            throw new InvalidSessionTierUpdateException("Parameter id and body id do not correspond");
        }

        SessionTier existingSessionTier = sessionTiersRepository.findById(id)
                .orElseThrow(() -> new SessionTierNotFoundException("Event not found"));

        //VERIFICAR SE ALTERA CORRETAMENTE

        return sessionTiersRepository.save(existingSessionTier);
    }

    public SessionTier getSessionTier(UUID sessionTierId) {
        return sessionTiersRepository
                .findById(sessionTierId)
                .orElseThrow(() -> new SessionTierNotFoundException("Event not found"));
    }

    public void deleteSessionTier(UUID id) {
        SessionTier sessionTierToDelete = sessionTiersRepository.findById(id).orElse(null);

        if (sessionTierToDelete == null) {
            throw new SessionTierNotFoundException("Event Session not found");
        }

        sessionTiersRepository.delete(sessionTierToDelete);
    }

}
