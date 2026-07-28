package com.elos.payments.payment_processing_service.service;

import com.elos.payments.payment_processing_service.dto.StatusNotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    public static final String PAYMENT_STATUS_QUEUE = "payment-status-updates";

    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendPaymentStatusUpdate(StatusNotificationDTO notification) {
        try {
            logger.info("Sending status update to queue [{}]: {}", PAYMENT_STATUS_QUEUE, notification);
            jmsTemplate.convertAndSend(PAYMENT_STATUS_QUEUE, notification);
        } catch (Exception e) {
            logger.error("Error sending message to queue: {}", PAYMENT_STATUS_QUEUE, e);
        }
    }
}