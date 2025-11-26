package org.evently.services;

import jakarta.transaction.Transactional;
import org.evently.exceptions.RefundDecisionNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.repositories.RefundDecisionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefundDecisionsService {

    @Autowired
    private RefundDecisionsRepository refundDecisionsRepository;

    public RefundDecision getRefundDecision(UUID id) {
        return refundDecisionsRepository
                .findById(id)
                .orElseThrow(() -> new RefundDecisionNotFoundException("Refund Decision not found"));
    }

    @Transactional
    public RefundDecision registerRefundDecision(RefundDecision refundDecision) {
        return refundDecisionsRepository.save(refundDecision);
    }

    public Page<RefundDecision> getRefundDecisionsByRequest(RefundRequest refundRequest, Integer pageNumber, Integer pageSize) {
        if(pageSize > 50){
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return refundDecisionsRepository.findAllByRefundRequest(refundRequest,pageable);
    }
}
