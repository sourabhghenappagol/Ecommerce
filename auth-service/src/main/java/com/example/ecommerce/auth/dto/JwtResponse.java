package com.example.ecommerce.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponse {
    private String token;
    private String username;
    private String email;
}
