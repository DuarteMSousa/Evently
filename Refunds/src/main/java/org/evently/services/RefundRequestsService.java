package org.evently.services;

import org.evently.exceptions.InvalidRefundRequestUpdateException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.repositories.RefundRequestsRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
public class RefundRequestsService {

    @Autowired
    private RefundRequestsRepository refundRequestsRepository;

    private ModelMapper modelMapper = new ModelMapper();

    public RefundRequest getRefundRequest(UUID id) {
        return refundRequestsRepository
                .findById(id)
                .orElseThrow(() -> new RefundRequestNotFoundException("Refund Request not found"));
    }

    @Transactional
    public RefundRequest createRefundRequest(RefundRequest refundRequest) {
        return refundRequestsRepository.save(refundRequest);
    }

    @Transactional
    public RefundRequest updateEvent(UUID id, RefundRequest event) {
        if (!id.equals(event.getId())) {
            throw new InvalidRefundRequestUpdateException("Parameter id and body id do not correspond");
        }

        RefundRequest existingEvent = refundRequestsRepository.findById(id)
                .orElseThrow(() -> new RefundRequestNotFoundException("Refund Request not found"));

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(event, existingEvent);

        return refundRequestsRepository.save(existingEvent);
    }

    public Page<RefundRequest> getRefundRequestsByUser(UUID userId, Integer pageNumber, Integer pageSize) {
        if(pageSize > 50){
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return refundRequestsRepository.findAllByUserId(userId,pageable);
    }
}
