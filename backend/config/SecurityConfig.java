/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;


/** 
 * Configuració central de Spring Security.
 * * Defineix la cadena de filtres de seguretat, habilita JWT (Estat sense sessió)
 * i autoritza l'accés als 'endpoints' públics.
 * 
 * @author David García Rodríguez
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    
    /**Filtre per JWT. */
    private final JwtFilter jwtFilter;
    /**Proveïdor d'autenticació. */
    private final AuthenticationProvider authenticationProvider;
    
    /**
     * David García Rodríguez
     * Estableix els filtres de seguretat de l'aplicació.
     * * Deshabilita CSRF, requereix autenticació i estableix rutes públiques
     * @param httpSecurity Configuració de seguretat.
     * @return Cadena de filtres.
     * @throws Exception Si hi ha un error de configuració.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        
        httpSecurity.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers(publicEndPoints()).permitAll()
                .anyRequest().authenticated())
                .logout(logout -> logout.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return httpSecurity.build();
    }
    
    /**
     * David García Rodríguez
     * Defineix les rutes que no necesiten autenticació (públics).
     * @return Un {@link RequestMatcher} combinat amb els 'endpoints'.
     */
    private RequestMatcher publicEndPoints() {
        return new OrRequestMatcher(           

                new AntPathRequestMatcher("/biblioteca/auth/login"),
                new AntPathRequestMatcher("/biblioteca/auth/afegirUsuari"),
                new AntPathRequestMatcher("/error")
                        );
    }
}
