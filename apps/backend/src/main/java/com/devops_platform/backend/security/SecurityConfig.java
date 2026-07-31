package com.devops_platform.backend.security;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests((requests) -> requests
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").hasRole("METRICS")
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("noop", NoOpPasswordEncoder.getInstance());
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_5());
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_2());

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