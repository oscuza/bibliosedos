/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.controller;

import com.bibliotecasedaos.biblioteca.entity.Llibre;
import com.bibliotecasedaos.biblioteca.error.LlibreNotFoundException;
import com.bibliotecasedaos.biblioteca.service.LlibreService;
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
 * Controlador REST per a la gestió dels recursos de l'entitat {@code Llibre}.
 * S'encarrega de mapejar les sol·licituds HTTP que comencen
 * per {@code /biblioteca/llibres}.
 * 
 * @author David García Rodríguez
 */
@RequestMapping("/biblioteca/llibres")
@RestController
public class LlibreController {
    
    @Autowired
    LlibreService llibreService;
    
    /**
     * Endpoint per recuperar una llista de tots els llibres disponibles.
     * @return Una llista d'objectes {@link Llibre}.
     */
    @GetMapping("/llistarLlibres")
    public List<Llibre> findAllLlibres() {
        return llibreService.findAllLlibres();
    }
    
    /**
     * Endpoint per afegir un nou llibre a la base de dades, requereix que l'usuari autenticat tingui l'autoritat 'ADMIN'.
     * @param llibre El cos de la sol·licitud que conté les dades del nou {@link Llibre}.
     * @return L'objecte {@link Llibre} desat, incloent-hi l'ID generat.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/afegirLlibre")
    public Llibre saveLlibre(@Valid @RequestBody Llibre llibre) {
        return llibreService.saveLlibre(llibre);
    }
    
    /**
     * Endpoint per actualitzar les dades d'un llibre existent, requereix que l'usuari autenticat tingui l'autoritat 'ADMIN'.
     * @param id L'identificador del llibre a actualitzar (obtingut del path de la URL).
     * @param llibre El cos de la sol·licitud amb les dades actualitzades del {@link Llibre}.
     * @return El llibre actualitzat.
     * @throws LlibreNotFoundException Si el llibre amb l'ID donat no existeix.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/actualitzarLlibre/{id}")
    public Llibre updateLlibre(@PathVariable Long id,@Valid @RequestBody Llibre llibre) throws LlibreNotFoundException {
        return llibreService.updateLlibre(id, llibre);
    }
    
    /**
     * Endpoint per eliminar un llibre de la base de dades, requereix que l'usuari autenticat tingui l'autoritat 'ADMIN'.
     * @param id L'identificador del llibre a eliminar.
     * @return Un missatge de confirmació d'eliminació.
     * @throws LlibreNotFoundException Si el llibre amb l'ID donat no existeix.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/eliminarLlibre/{id}")
    public String deleteLlibre(@PathVariable Long id) throws LlibreNotFoundException {
        llibreService.deleteLlibre(id);
        return "Llibre esborrat";
    }
    
    /**
     * Endpoint per buscar un llibre per la seva clau primària (ID).
     * @param id L'identificador del llibre a buscar.
     * @return L'objecte {@link Llibre} trobat.
     * @throws LlibreNotFoundException Si no es troba cap llibre amb l'ID donat.
     */
    @GetMapping("/trobarLlibrePerId/{id}")
    Llibre findLlibreById(@PathVariable Long id) throws LlibreNotFoundException {
        return llibreService.findLlibreById(id);
    }
}
