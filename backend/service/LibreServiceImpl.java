/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Autor;
import com.bibliotecasedaos.biblioteca.entity.Llibre;
import com.bibliotecasedaos.biblioteca.error.LlibreNotFoundException;
import com.bibliotecasedaos.biblioteca.repository.AutorRepository;
import com.bibliotecasedaos.biblioteca.repository.LlibreRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Classe que proporciona la lògica de negoci per a la gestió dels llibres, actuant com a intermediari entre el controlador i el repositori.
 * 
 * @author David García Rodríguez
 */
@Service
public class LibreServiceImpl implements LlibreService{
   
    @Autowired
    LlibreRepository llibreRepository;

    @Autowired
    AutorRepository autorRepository;

    /**
     * Recupera una llista de tots els llibres presents a la base de dades, ordenats pel títol.
     * @return Una llista d'objectes {@code Llibre}. Retorna una llista buida si no hi ha llibres registrats.
     */
    @Override
    public List<Llibre> findAllLlibres() {
        return llibreRepository.findAllByOrderByTitolAsc(); 
    }
  
    /**
     * Desa un nou llibre o actualitza un llibre existent a la base de dades.
     * @param llibre L'objecte {@code Llibre} a desar o actualitzar.
     * @return L'objecte {@code Llibre} que ha estat desat.
     */
    @Override
    public Llibre saveLlibre(Llibre llibre) {
        return llibreRepository.save(llibre);
    }

    /**
     * Actualitza un llibre existent amb les dades proporcionades.
     * @param id L'identificador del llibre a actualitzar.
     * @param llibre L'objecte {@code Llibre} amb les noves dades a aplicar.
     * @return El llibre actualitzat.
     * @throws LlibreNotFoundException Si el llibre amb l'ID donat no existeix.
     */
    @Override
    public Llibre updateLlibre(Long id, Llibre llibre) throws LlibreNotFoundException {


        Llibre llibreDb = llibreRepository.findById(id)
                .orElseThrow(() -> new LlibreNotFoundException("Llibre amb ID " + id + " no trobat"));

        llibreDb.setIsbn(llibre.getIsbn());
        llibreDb.setTitol(llibre.getTitol());
        llibreDb.setPagines(llibre.getPagines());
        llibreDb.setEditorial(llibre.getEditorial());

        if (llibre.getAutor() != null && llibre.getAutor().getId() != null) {
            Autor autor = autorRepository.findById(llibre.getAutor().getId())
                    .orElse(null);
            llibreDb.setAutor(autor);
        } else {
            llibreDb.setAutor(null);
        }

        return llibreRepository.save(llibreDb);
        /*
        Llibre llibreDb = llibreRepository.findById(id)
             .orElseThrow(() -> new LlibreNotFoundException("Llibre amb ID " + id + " no trobat."));
        
        if (Objects.nonNull(llibre.getTitol()) && !llibre.getTitol().isBlank()) {
            llibreDb.setTitol(llibre.getTitol());
        }
        
        return llibreRepository.save(llibreDb);
        */
    }
   
    /**
     * Elimina un llibre de la base de dades mitjançant el seu identificador.
     * @param id L'identificador del llibre a eliminar.
     * @throws LlibreNotFoundException Si el llibre amb l'ID donat no es troba.
     */
    @Override
    public void deleteLlibre(Long id) throws LlibreNotFoundException {
        llibreRepository.findById(id)
            .orElseThrow(() -> new LlibreNotFoundException("Llibre amb ID " + id + " no trobat."));
        
        llibreRepository.deleteById(id);
    }

    /**
     * Busca i recupera un llibre específic mitjançant el seu identificador.
     * @param id L'identificador del llibre a buscar.
     * @return L'objecte {@code Llibre} corresponent a l'ID.
     * @throws LlibreNotFoundException Si el llibre amb l'ID donat no es troba.
     */
    @Override
    public Llibre findLlibreById(Long id) throws LlibreNotFoundException {
        return llibreRepository.findById(id)
            .orElseThrow(() -> new LlibreNotFoundException("Llibre amb ID " + id + " no trobat."));
    }
}
