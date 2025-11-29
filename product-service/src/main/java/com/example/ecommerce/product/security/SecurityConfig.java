package com.example.ecommerce.product.security;

import com.example.ecommerce.product.config.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize roles
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Create JWT filter object
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil);

        http
                .csrf(csrf -> csrf.disable())

                // disable default form login and http basic so browser won't show popup
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())

                // stateless session (no HTTP session)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // return a simple JSON 401 without WWW-Authenticate header to avoid browser basic auth popup
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                }))

                .authorizeHttpRequests(auth -> auth
                        // ALLOW SWAGGER WITHOUT AUTH (include common swagger/static paths)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/configuration/**",
                                "/webjars/**",
                                "/actuator/**",
                                "/public/**"
                        ).permitAll()

                        // ALL OTHER ENDPOINTS REQUIRE AUTH
                        .anyRequest().authenticated()
                )

                // ADD JWT FILTER BEFORE USERNAME/PASSWORD FILTER
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // additionally ignore swagger related static resources at the web security level
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/swagger-resources/**",
                "/webjars/**"
        );
    }
}
