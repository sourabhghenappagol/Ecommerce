package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.PaymentRequest;
import com.ecommerce.orderservice.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${payment.service.url:http://localhost:8090}")
    private String paymentServiceUrl;

    public PaymentServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        String url = paymentServiceUrl + "/api/payments/process";

        try {
            System.out.println("Calling Payment Service at: " + url);
            ResponseEntity<PaymentResponse> response =
                    restTemplate.postForEntity(url, request, PaymentResponse.class);

            return response.getBody();

        } catch (HttpClientErrorException e) {
            // Handle HTTP 402 Payment Required (payment failed)
            if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                System.out.println("Payment failed with status 402");
                // Parse the response body
                return e.getResponseBodyAs(PaymentResponse.class);
            }
            throw new RuntimeException("Payment service error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to communicate with Payment Service: " + e.getMessage(), e);
        }
    }
}