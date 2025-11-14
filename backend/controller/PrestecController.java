/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.controller;

import com.bibliotecasedaos.biblioteca.entity.Prestec;
import com.bibliotecasedaos.biblioteca.error.ExemplarNotFoundException;
import com.bibliotecasedaos.biblioteca.error.ExemplarReservatException;
import com.bibliotecasedaos.biblioteca.error.PrestecNotFoundException;
import com.bibliotecasedaos.biblioteca.service.PrestecService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió de préstecs, proporciona els endpoints necessaris.
 * @author David García Rodríguez
 */
@RequestMapping("/biblioteca/prestecs")
@RestController
public class PrestecController {

    private final PrestecService prestecService;

    /**
     * Constructor amb injecció de dependència del servei de préstecs.
     * @param prestecService El servei de negoci per a les operacions de préstec.
     */
    @Autowired
    public PrestecController(PrestecService prestecService) {
        this.prestecService = prestecService;
    }

    /**
     * Endpoint per llistar TOTS els préstecs actius d'un usuari específic, o de tots els usuaris.
     * Només pot ser accedit per l'ADMIN o per l'USUARI propi.
     * @param usuariId L'ID de l'usuari.
     * @return Llista de préstecs actius.
     */
    @PreAuthorize("hasAuthority('ADMIN') or (#usuariId != null and #usuariId == authentication.principal.id)")
    @GetMapping("/llistarPrestecsActius")
    public List<Prestec> findPrestecsActius(@RequestParam(required = false) Long usuariId) {
        return prestecService.findPrestecsActius(usuariId);
    }

    /**
     * Endpoint per llistar TOTS els préstecs d'un usuari específic o de tots el usuaris.
     * Només pot ser accedit per l'ADMIN o per l'USUARI propi.
     * @param usuariId L'ID de l'usuari.
     * @return Llista de tots els préstecs.
     */
    @PreAuthorize("hasAuthority('ADMIN') or #usuariId == authentication.principal.id")
    @GetMapping("/llistarPrestecs")
    public List<Prestec> findAllPrestecs(@RequestParam(required = false) Long usuariId) {
        return prestecService.findAllPrestecs(usuariId);
    } 

    
    /**
     * Endpoint per registrar un nou préstec.
     * Només accessible per l'ADMIN.
     * @param prestec El cos de la sol·licitud amb el nou {@link Prestec}.
     * @return El préstec creat amb l'ID.
     */
    
    //@PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/afegirPrestec")
    public ResponseEntity<Prestec> savePrestec(@Valid @RequestBody Prestec prestec) throws ExemplarReservatException, ExemplarNotFoundException {
        Prestec nouPrestec = prestecService.savePrestec(prestec);
        return new ResponseEntity<>(nouPrestec, HttpStatus.CREATED);
    }


    /**
     * Endpoint per marcar un préstec com a retornat (actualitzant dataDevolucio a la data actual).
     * Només accessible per l'ADMIN.
     * @param prestecId L'ID del préstec a retornar.
     * @return Una resposta d'èxit sense contingut (204 No Content).
     * @throws PrestecNotFoundException Si el préstec amb l'ID donat no existeix.
     */
   // @PreAuthorize("hasAuthority('ADMIN') or @prestecServiceImpl.isPrestecOwner(#prestecId, principal)")
    @PutMapping("/ferDevolucio/{prestecId}")
    public String retornarPrestec(@PathVariable Long prestecId) 
            throws PrestecNotFoundException {       
        prestecService.retornarPrestec(prestecId);
        return "Prestec retornat";
        
    }
}
