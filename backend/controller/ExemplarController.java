/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.controller;

import com.bibliotecasedaos.biblioteca.entity.Exemplar;
import com.bibliotecasedaos.biblioteca.entity.Llibre;
import com.bibliotecasedaos.biblioteca.error.ExemplarNotFoundException;
import com.bibliotecasedaos.biblioteca.service.ExemplarService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió dels recursos de l'entitat {@code Exemplar}.
 * S'encarrega de mapejar les sol·licituds HTTP que comencen per {@code /biblioteca/exemplars}
 * 
 * @author David García Rodríguez
 */
@RequestMapping("/biblioteca/exemplars")
@RestController
public class ExemplarController {
    
    @Autowired
    ExemplarService exemplarService;
    
    /**
     * Endpoint per recuperar una llista de tots els exemplars de llibres registrats.
     * @return Una llista d'objectes {@link Exemplar}.
     */
    @GetMapping("/llistarExemplars")
    public List<Exemplar> findAllExemplars() {
        return exemplarService.findAllExemplars();
    }
    
    /**
     * Endpoint per buscar exemplars disponibles (lliures per préstec) per títol de llibre o nom d'autor.
     * @param titol Paràmetre opcional per filtrar pel títol del llibre.
     * @param autorNom Paràmetre opcional per filtrar pel nom de l'autor.
     * @return Una llista d'exemplars {@link Exemplar} lliures que coincideixen amb els criteris.
     */
    @GetMapping("/llistarExemplarsLliures")
    public List<Exemplar> findExemplarsByTitolOrAutor(
        @RequestParam(required = false) String titol,
        @RequestParam(required = false, name = "autor") String autorNom 
    ) {
        return exemplarService.findExemplarsLliuresByTitolOrAutor(titol, autorNom);
    }
    
    /**
     * Endpoint per afegir un nou exemplar a la base de dades, requereix que l'usuari autenticat tingui l'autoritat 'ADMIN'.
     * @param exemplar El cos de la sol·licitud que conté les dades del nou {@link Exemplar}.
     * @return L'objecte {@link Exemplar} desat, incloent-hi l'ID generat.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/afegirExemplar")
    public Exemplar saveExemplar(@RequestBody Exemplar exemplar) {
        return exemplarService.saveExemplar(exemplar);
    }
    
    /**
     * Endpoint per actualitzar les dades d'un exemplar existent.
     * @param id L'identificador de l'exemplar a actualitzar.
     * @param exemplar El cos de la sol·licitud amb les dades actualitzades.
     * @return L'exemplar actualitzat.
     * @throws ExemplarNotFoundException Si l'exemplar amb l'ID donat no existeix.
     */
    //@PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/actualitzarExemplar/{id}")
    public Exemplar updateExemplar(@PathVariable Long id,@RequestBody Exemplar exemplar) throws ExemplarNotFoundException {
        return exemplarService.updateExemplar(id, exemplar);
    }
    
    /**
     * Endpoint per eliminar un exemplar de la base de dades, requereix que l'usuari autenticat tingui l'autoritat 'ADMIN'.
     * @param id L'identificador de l'exemplar a eliminar.
     * @return Un missatge de confirmació d'eliminació.
     * @throws ExemplarNotFoundException Si l'exemplar amb l'ID donat no existeix.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/eliminarExemplar/{id}")
    public String deleteExemplar(@PathVariable Long id) throws ExemplarNotFoundException {
        exemplarService.deleteExemplar(id);
        return "Exemplar esborrat";
    }
    
    /**
     * Endpoint per buscar un exemplar per la seva clau primària (ID).
     * @param id L'identificador de l'exemplar a buscar.
     * @return L'objecte {@link Exemplar} trobat.
     * @throws ExemplarNotFoundException Si no es troba cap exemplar amb l'ID donat.
     */
    @GetMapping("/trobarExemplarPerId/{id}")
    Exemplar findExemplarById(@PathVariable Long id) throws ExemplarNotFoundException {
        return exemplarService.findExemplarById(id);
    }
}
