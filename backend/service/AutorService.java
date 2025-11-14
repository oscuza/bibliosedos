/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Autor;
import com.bibliotecasedaos.biblioteca.error.AutorNotFoundException;
import java.util.List;

/**
 * Interfície de servei per a la gestió d'autors a l'aplicació.
 * Defineix les operacions bàsiques de CRUD
 * 
 * @author David García Rodríguez
 */
public interface AutorService {
    
    /**
     * Recupera una llista de tots els autors presents a la base de dades.
     * @return {@link Autor}
     */
    List<Autor> findAllAutors();
    
    /**
     * Troba un autor pel seu id.
     * @param id L'identificador (ID) de l'autor.
     * @return L'objecte {@link Autor} que ha trobat.
     * @throws com.bibliotecasedaos.biblioteca.error.AutorNotFoundException
     */
    Autor findAutorById(Long id) throws AutorNotFoundException;
    
    /**
     * Guarda o actualitza un objecte Autor a la base de dades.
     * @param autor L'objecte {@link Autor} a desar o actualitzar.
     * @return L'objecte {@link Autor} que ha estat desat).
     */
    Autor saveAutor(Autor autor);
    
    /**
     * Elimina un autor de la base de dades pel seu id.
     * @param id L'identificador (ID) de l'autor a eliminar.
     * @throws AutorNotFoundException Si l'autor amb l'ID especificat no es troba a la base de dades.
     */
    void deleteAutor(long id) throws AutorNotFoundException;
    
}
