package org.evently.services;

import org.evently.exceptions.UnexistingRefundRequestMessageException;
import org.evently.models.RefundRequestMessage;
import org.evently.repositories.RefundRequestMessagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
public class RefundRequestMessagesService {

    @Autowired
    private RefundRequestMessagesRepository refundRequestMessagesRepository;

    public RefundRequestMessage getRefundRequestMessage(UUID id) throws UnexistingRefundRequestMessageException {
        return refundRequestMessagesRepository
                .findById(id)
                .orElseThrow(() -> new UnexistingRefundRequestMessageException());
    }

    @Transactional
    public RefundRequestMessage sendRefundRequestMessage(RefundRequestMessage refundRequestMessage) {
        return refundRequestMessagesRepository.save(refundRequestMessage);
    }
}
