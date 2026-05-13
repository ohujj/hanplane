package com.hanplane.domain.payment.service;

import com.hanplane.domain.order.repository.OrderRepository;
import com.hanplane.domain.payment.dto.PaymentConfirmRequest;
import com.hanplane.domain.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentConfirmService paymentConfirmService;

    public void confirm(PaymentConfirmRequest request) {
        Payment payment = paymentConfirmService.confirmOrder(request);

        Portone
    }








    }




}
