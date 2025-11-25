package org.evently.services;

import org.evently.exceptions.UnexistingRefundRequestException;
import org.evently.models.RefundRequest;
import org.evently.repositories.RefundRequestsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
public class RefundRequestsService {

    @Autowired
    private RefundRequestsRepository refundRequestsRepository;

    public RefundRequest getRefundRequest(UUID id) throws UnexistingRefundRequestException {
        return refundRequestsRepository
                .findById(id)
                .orElseThrow(() -> new UnexistingRefundRequestException());
    }

    @Transactional
    public RefundRequest createRefundRequest(RefundRequest refundRequest) {
        return refundRequestsRepository.save(refundRequest);
    }
}
