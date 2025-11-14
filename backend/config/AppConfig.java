/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.config;

import com.bibliotecasedaos.biblioteca.repository.UsuariRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 
 * Configuració central de Spring Security. Defineix els 'beans' essencials
 * per a l'autenticació i la seguretat de l'aplicació.
 * 
 * @author David García Rodríguez
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {
    
    private final UsuariRepository usuariRepository;
    
    /**
     * David García Rodríguez
     * Defineix el servei per trobar un usuari per nick.
     * @return {@link UserDetailsService}.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return (String username) -> usuariRepository.findUsuariByNickWithJPQL(username)
                .orElseThrow(()-> new UsernameNotFoundException("Usuari no trobat"));
    }
    
    /**
     * David García Rodríguez
     * Defineix el proveïdor d'autenticació amb el servei d'usuari i l'algoritme d'encriptació.
     * @return {@link DaoAuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }
    
    /**
     * David García Rodríguez 
     * Defineix el codificador de contrasenyes.
     * @return {@link BCryptPasswordEncoder}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * David García Rodríguez
     * Defineix l'administrador d'autenticació de Spring Security.
     * @param config La configuració base.
     * @return {@link AuthenticationManager}.
     * @throws Exception Si falla la configuració.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }
}
