/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.controller;

import com.bibliotecasedaos.biblioteca.entity.Usuari;
import com.bibliotecasedaos.biblioteca.error.UsuariNotFoundException;
import com.bibliotecasedaos.biblioteca.service.UsuariService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Classe on es defineixen el diferents endpoints per a la gestió dels usuaris.
 * Prporciona operacions CRUD per satisfer les peticions dels clients.
 * 
 * @author David García Rodríguez
 */
@RequestMapping("/biblioteca/usuaris")
@RestController
public class UsuariController {
    
    @Autowired
    UsuariService usuariService;
    
    /**
     * Endpoint per a trobar un usuari pel seu identificador únic (ID).
     * Requereix el rol d'ADMINISTRADOR.
     *
     * @param id L'identificador únic de l'usuari a buscar.
     * @return L'objecte {@link Usuari} trobat.
     * @throws UsuariNotFoundException Si no es troba cap usuari amb l'ID donat.
     */
    
    @PreAuthorize("hasAuthority('ADMIN') or (isAuthenticated() and @usuariServiceImpl.isResourceOwner(#id, principal))")
    @GetMapping("/trobarUsuariPerId/{id}")
    Usuari findUsuariById(@PathVariable Long id) throws UsuariNotFoundException{
        return usuariService.findUsuariById(id);
    }
    
    /**
     * Endpoint per a buscar un usuari pel seu nom d'usuari (nick).
     * Requereix el rol d'ADMINISTRADOR.
     *
     * @param nick El nom d'usuari (nick) a buscar.
     * @return Un {@link Optional} que conté l'objecte {@link Usuari} si existeix.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/trobarUsuariPerNick/{nick}")
    Optional<Usuari> findUsuariByNickWithJPQL(@PathVariable String nick) throws UsuariNotFoundException {
        return usuariService.findUsuariByNameWithJPQL(nick);
    }
    
    /**
     * Endpoint per a buscar un usuari pel seu NIF (nif).
     * Requereix el rol d'ADMINISTRADOR.
     *
     * @param nif El NIF de l'usuari a buscar.
     * @return Un {@link Optional} que conté l'objecte {@link Usuari} si existeix.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/trobarUsuariPerNif/{nif}")
    Optional<Usuari> findUsuariByNifWithJPQL(@PathVariable String nif) {
        return usuariService.findByNif(nif);
    }
    
    //¿eliminar?
    @GetMapping("/trobarUsuariPerNifJ/{nif}")
    Optional<Usuari> findUsuariByNifWith(@PathVariable String nif) {
        return usuariService.findUsuariByNifWithJPQL(nif);
    }
    
    /**
     * Endpoint per a llistar tots els usuaris registrats a la base de dades.
     * Requereix el rol d'ADMINISTRADOR.
     *
     * @return Una {@link List} que conté tots els objectes {@link Usuari} del sistema.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/llistarUsuaris")
    public List<Usuari> findAllUsuaris() {
        return usuariService.findAllUsuaris();
    }
    
    /**
     * Endpoint per a actualitzar les dades d'un usuari existent.El cos de la petició (RequestBody) conté les noves dades.
     *
     * @param id L'identificador (ID) de l'usuari a actualitzar.
     * @param usuari L'objecte {@link Usuari} amb les dades a aplicar.
     * @return L'objecte {@link Usuari} actualitzat.
     * @throws com.bibliotecasedaos.biblioteca.error.UsuariNotFoundException
     */
    @PutMapping("/actualitzarUsuari/{id}")
    public Usuari updateUsuari(@PathVariable Long id,@RequestBody Usuari usuari) throws UsuariNotFoundException{
        return usuariService.updateUsuari(id, usuari);
    }
    
    /**
     * Endpoint per a eliminar un usuari de la base de dades pel seu identificador.
     *
     * @param id L'identificador (ID) de l'usuari a eliminar.
     * @return Un missatge de confirmació indicant que l'usuari ha estat esborrat.
     */
    @DeleteMapping("/eliminarUsuari/{id}")
    public String deleteUsuari(@PathVariable Long id) {
        usuariService.deleteUsuari(id);
        return "Usuari esborrat";
    }
}
