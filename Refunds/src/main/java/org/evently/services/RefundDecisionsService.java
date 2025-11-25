package org.evently.services;

import org.evently.exceptions.UnexistingRefundDecisionException;
import org.evently.models.RefundDecision;
import org.evently.repositories.RefundDecisionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
public class RefundDecisionsService {

    @Autowired
    private RefundDecisionsRepository refundDecisionsRepository;

    public RefundDecision getRefundDecision(UUID id) throws UnexistingRefundDecisionException {
        return refundDecisionsRepository
                .findById(id)
                .orElseThrow(() -> new UnexistingRefundDecisionException());
    }

    @Transactional
    public RefundDecision registerRefundDecision(RefundDecision refundDecision) {
        return refundDecisionsRepository.save(refundDecision);
    }
}
