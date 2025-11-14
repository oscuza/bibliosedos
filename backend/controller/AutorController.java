/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.controller;

import com.bibliotecasedaos.biblioteca.entity.Autor;
import com.bibliotecasedaos.biblioteca.entity.Usuari;
import com.bibliotecasedaos.biblioteca.error.AutorNotFoundException;
import com.bibliotecasedaos.biblioteca.error.UsuariNotFoundException;
import com.bibliotecasedaos.biblioteca.service.AutorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió dels recursos de l'entitat {@code Autor}.
 * S'encarrega de mapejar les sol·licituds HTTP que comencen per {@code /biblioteca/autors}.
 * 
 * @author David García Rodríguez
 */
@RequestMapping("/biblioteca/autors")
@RestController
public class AutorController {
    
    @Autowired
    AutorService autorService;
    
    /**
     * Endpoint per recuperar una llista de tots els autors disponibles.
     * @return Una llista d'objectes {@link Autor} ordenats.
     */
    @GetMapping("/llistarAutors")
    public List<Autor> findAllAutors() {
        return autorService.findAllAutors();
    }
    
    /**
     * Endpoint per buscar un autor per la seva clau primària (ID).
     * @param id L'identificador de l'autor a buscar.
     * @return L'objecte {@link Autor} trobat.
     * @throws AutorNotFoundException Si no es troba cap autor amb l'ID donat.
     */
    @GetMapping("/trobarAutorPerId/{id}")
    Autor findAutorById(@PathVariable Long id) throws AutorNotFoundException {
        return autorService.findAutorById(id);
    }
    
    /**
     * Endpoint per eliminar un autor de la base de dades mitjançant el seu ID, requereix que l'usuari autenticat tingui l'autoritat 'ADMIN'.
     * @param id L'identificador de l'autor a eliminar.
     * @return Un missatge de confirmació d'eliminació.
     * @throws AutorNotFoundException Si l'autor amb l'ID donat no existeix.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/eliminarAutor/{id}")
    public String deleteAutor(@PathVariable Long id) throws AutorNotFoundException{
        autorService.deleteAutor(id);
        return "Autor esborrat";
    }
    
    /**
     * Endpoint per afegir un nou autor a la base de dades, requereix que l'usuari autenticat tingui l'autoritat 'ADMIN'.
     * @param autor El cos de la sol·licitud que conté les dades del nou {@link Autor}.
     * @return L'objecte {@link Autor} desat, incloent-hi l'ID generat.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/afegirAutor")
    public Autor saveAutor(@Valid @RequestBody Autor autor) {
        return autorService.saveAutor(autor);
    }
    
    
}
