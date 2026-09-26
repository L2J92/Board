package com.example.board.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            ObjectMapper objectMapper
    ) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable)
                    .sessionManagement(session->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .requestCache(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth->auth
                            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                            .requestMatchers(HttpMethod.POST,"/member").permitAll()
                            .requestMatchers(HttpMethod.POST,"/auth/login").permitAll()
                            .requestMatchers(HttpMethod.GET,
                                    "/post", "/post/**",
                                    "/comment","/comment/**"
                            ).permitAll()
                            .requestMatchers(
                                    "/swagger-ui.html",
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**"
                            ).permitAll()
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(exception->
                                    exception.authenticationEntryPoint(
                                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                                    )
                            );

                    http.addFilterBefore(
                      new JwtAuthenticationFilter(jwtTokenProvider, objectMapper),
                      UsernamePasswordAuthenticationFilter.class
                    );

                    return  http.build();
    }

}
