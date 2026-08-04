package com.devops_platform.backend.security;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${PASSWORD_ENCODER_ID:bcrypt}")
    private String encoderId;

    @Value("${APP_ADMIN_USER:admin}")
    private String adminUser;

    @Value("${APP_ADMIN_PASSWORD:adminpass}")
    private String adminPassword;

    @Value("${PROMETHEUS_SCRAPE_USER:prometheus}")
    private String prometheusUser;

    @Value("${PROMETHEUS_SCRAPE_PASSWORD:prometheuspass}")
    private String prometheusPassword;

    @Bean
    @Order(1)
    public SecurityFilterChain basicSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/actuator/health", "/actuator/prometheus")
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus", "/actuator/info").hasRole("METRICS")
                .anyRequest().denyAll())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }
    @Bean
    @Order(2)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2ResourceServer((oauth2) -> oauth2
				.jwt(Customizer.withDefaults())
			)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login/**").permitAll()
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }   

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        DelegatingPasswordEncoder delegatingEncoder = new DelegatingPasswordEncoder(encoderId, encoders);
        delegatingEncoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());

        return delegatingEncoder;
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.withUsername(adminUser)
            .password(encoder.encode(adminPassword))
            .roles("USER", "ADMIN")
            .build();
        UserDetails prometheus = User.withUsername(prometheusUser)
            .password(encoder.encode(prometheusPassword))
            .roles("METRICS")
            .build();
        return new InMemoryUserDetailsManager(admin, prometheus);
    }
}