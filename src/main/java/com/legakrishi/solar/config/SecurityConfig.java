package com.legakrishi.solar.config;

import com.legakrishi.solar.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Use DAO provider
                .authenticationProvider(authenticationProvider())

                // CSRF: allow device/websocket/actuator without token
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        new AntPathRequestMatcher("/api/iot/**"),
                        new AntPathRequestMatcher("/ws/**"),
                        new AntPathRequestMatcher("/actuator/**")
                ))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // health + info
                                antMatcher("/actuator/health"),
                                antMatcher("/actuator/info"),

                                // public pages
                                antMatcher("/login"),
                                antMatcher("/register"),

                                // static assets
                                antMatcher("/css/**"),
                                antMatcher("/js/**"),
                                antMatcher("/images/**"),
                                antMatcher("/img/**"),
                                antMatcher("/webjars/**"),
                                antMatcher("/assets/**"),
                                antMatcher("/favicon.ico"),
                                antMatcher("/robots.txt"),

                                // websocket + ingest apis (public by design)
                                antMatcher("/ws/**"),
                                antMatcher("/api/iot/**")
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Form login + logout
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .permitAll()
                );

        return http.build();
    }
}
