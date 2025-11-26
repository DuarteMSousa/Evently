package org.evently.services;

import jakarta.transaction.Transactional;
import org.evently.exceptions.RefundRequestMessageNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.models.RefundRequestMessage;
import org.evently.repositories.RefundRequestMessagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefundRequestMessagesService {

    @Autowired
    private RefundRequestMessagesRepository refundRequestMessagesRepository;

    public RefundRequestMessage getRefundRequestMessage(UUID id) {
        return refundRequestMessagesRepository
                .findById(id)
                .orElseThrow(() -> new RefundRequestMessageNotFoundException("Refund Request Message not found"));
    }

    @Transactional
    public RefundRequestMessage sendRefundRequestMessage(RefundRequestMessage refundRequestMessage) {
        return refundRequestMessagesRepository.save(refundRequestMessage);
    }

    public Page<RefundRequestMessage> getRefundRequestMessagesByRequest(RefundRequest refundRequest, Integer pageNumber, Integer pageSize) {
        if(pageSize > 50){
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return refundRequestMessagesRepository.findAllByRefundRequest(refundRequest,pageable);
    }
}
