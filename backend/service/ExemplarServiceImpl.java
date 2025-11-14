/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Exemplar;
import com.bibliotecasedaos.biblioteca.entity.Llibre;
import com.bibliotecasedaos.biblioteca.error.ExemplarNotFoundException;
import com.bibliotecasedaos.biblioteca.repository.ExemplarRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Classe que proporciona la lògica de negoci per a la gestió dels exemplars, implementant les operacions de cerca i manteniment de dades.
 * 
 * @author David García Rodríguez
 */
@Service
public class ExemplarServiceImpl implements ExemplarService{

    @Autowired
    ExemplarRepository exemplarRepository;
    
    /**
     * Recupera una llista de tots els exemplars de llibres existents a la base de dades.
     * @return Una llista d'objectes {@link Exemplar}.
     */
    @Override
    public List<Exemplar> findAllExemplars() {
        return exemplarRepository.findAll();
    }

    /**
     * Desa un nou exemplar o actualitza un exemplar existent a la base de dades.
     * @param exemplar L'objecte {@link Exemplar} a desar o actualitzar.
     * @return L'objecte {@link Exemplar} que ha estat desat.
     */
    @Override
    public Exemplar saveExemplar(Exemplar exemplar) {
        return exemplarRepository.save(exemplar);
    }

    /**
     * Actualitza un exemplar existent amb les dades proporcionades de lloc i reservat.
     * @param id L'identificador de l'exemplar a actualitzar.
     * @param exemplar L'objecte {@link Exemplar} amb les noves dades a aplicar.
     * @return L'exemplar actualitzat i desat a la base de dades.
     * @throws ExemplarNotFoundException Si l'exemplar amb l'ID donat no existeix.
     */
    @Override
    public Exemplar updateExemplar(Long id, Exemplar exemplar) throws ExemplarNotFoundException {
        Exemplar exemplarDb = exemplarRepository.findById(id)
                .orElseThrow(() -> new ExemplarNotFoundException("Exemplar amb ID " + id + " no trobat."));
        if (Objects.nonNull(exemplar.getReservat()) && !"".equalsIgnoreCase(exemplar.getReservat())) {
            exemplarDb.setReservat(exemplar.getReservat());
        }
        
        if (Objects.nonNull(exemplar.getLloc()) && !"".equalsIgnoreCase(exemplar.getLloc())) {
            exemplarDb.setLloc(exemplar.getLloc());
        }
        
        return exemplarRepository.save(exemplarDb);
    }

    /**
     * Elimina un exemplar de la base de dades mitjançant el seu identificador.
     * @param id L'identificador de l'exemplar a eliminar.
     * @throws ExemplarNotFoundException Si l'exemplar amb l'ID donat no es troba a la base de dades.
     */
    @Override
    public void deleteExemplar(Long id) throws ExemplarNotFoundException {
        exemplarRepository.findById(id)
                .orElseThrow(() -> new ExemplarNotFoundException("Exemplar amb ID " + id + " no trobat."));
        
        exemplarRepository.deleteById(id);
    }

    /**
     * Busca i recupera un exemplar específic mitjançant el seu identificador.
     * @param id L'identificador de l'exemplar a buscar.
     * @return L'objecte {@link Exemplar} corresponent a l'ID.
     * @throws ExemplarNotFoundException Si l'exemplar amb l'ID donat no es troba.
     */
    @Override
    public Exemplar findExemplarById(Long id) throws ExemplarNotFoundException{
        return exemplarRepository.findById(id)
                .orElseThrow(() -> new ExemplarNotFoundException("Exemplar amb ID " + id + " no trobat."));
    }

    /**
     * Busca exemplars disponibles (lliures per préstec) basant-se en el títol del llibre
     * o el nom de l'autor.
     * @param titol El títol del llibre (o part del títol) per filtrar. Pot ser {@code null} o buit.
     * @param autorNom El nom de l'autor (o part del nom) per filtrar. Pot ser {@code null} o buit.
     * @return Una llista d'exemplars {@link Exemplar} que estan disponibles i coincideixen amb els criteris de cerca.
     */
    @Override
    public List<Exemplar> findExemplarsLliuresByTitolOrAutor(String titol, String autorNom) {

        if (Objects.nonNull(titol) && !titol.isBlank()) {
            return exemplarRepository.findExemplarsLliuresByLlibreTitol(titol);
        } else if (Objects.nonNull(autorNom) && !autorNom.isBlank()) {
            return exemplarRepository.findExemplarsLliuresByAutorNom(autorNom);          
        } else {
            return exemplarRepository.findExemplarsLliures();
        }
    }
    
}
