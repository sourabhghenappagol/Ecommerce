package com.ecommerce.cartservice.controller;


import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id="status")
public class CustomStatsEndpoint {

    @ReadOperation
    public String Status()
    {
        return "Service is up and running";
    }
}
