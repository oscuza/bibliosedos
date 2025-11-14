/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Autor;
import com.bibliotecasedaos.biblioteca.error.AutorNotFoundException;
import com.bibliotecasedaos.biblioteca.repository.AutorRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Classe que proporciona la lògica de negoci per a la gestió dels autors, actuant com a pont entre el controlador i el repositori.
 * 
 * @author David García Rodríguez
 */
@Service
public class AutorServiceImpl implements AutorService{

    @Autowired
    AutorRepository autorRepository;
    
    /**
     * Recupera una llista de tots els autors presents a la base de dades, ordenats de forma ascendent pel nom. 
     * * @return Una llista d'objectes {@link Autor}.
     */
    @Override
    public List<Autor> findAllAutors() {
        return autorRepository.findAllByOrderByNomAsc();
    }
    
    /**
     * Recupera un autor mitjançant el seu identificador.
     * @param id L'identificador de l'autor a buscar.
     * @return L'objecte Autor trobat.
     * @throws AutorNotFoundException Si l'autor amb l'ID donat no existeix.
     */
    @Override
    public Autor findAutorById(Long id) throws AutorNotFoundException {
        Optional<Autor> autor = autorRepository.findById(id);
        
        if (autor.isEmpty()) {
            throw new AutorNotFoundException("L'autor amb ID " + id + " no s'ha trobat.");
        }
        return autor.get();
    }

    /**
     * Guarda o actualitza un objecte Autor a la base de dades.
     * @param autor L'objecte {@link Autor} a desar o actualitzar.
     * @return L'objecte {@link Autor} que ha estat desat.
     */
    @Override
    public Autor saveAutor(Autor autor) {
        return  autorRepository.save(autor);
    }

    /**
     * Elimina un autor de la base de dades mitjançant el seu identificador.
     * <p>Abans d'eliminar, verifica si l'autor existeix. Si no existeix,
     * llança una excepció {@link AutorNotFoundException}.</p>
     * * @param id L'identificador (ID) de l'autor a eliminar.
     * @throws AutorNotFoundException Si l'autor amb l'ID especificat no es troba.
     */
    @Override
    public void deleteAutor(long id) throws AutorNotFoundException{
        if (!autorRepository.findById(id).isPresent()) {
            throw new AutorNotFoundException("Autor no trobat.");
        } else {
            autorRepository.deleteById(id);
        }
               
    }
    
}
