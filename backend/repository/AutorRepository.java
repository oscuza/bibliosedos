/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.repository;

import com.bibliotecasedaos.biblioteca.entity.Autor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori de dades per a l'entitat Autor, que proporcionar mètodes de gestió de dades (Crud).
 *
 * @author David García Rodríguez
 */
@Repository
public interface AutorRepository extends JpaRepository<Autor, Long>{
    
    /**
     * Recupera una llista de tots els autors, ordenats alfabèticament pel seu nom (nom).
     * @return Llista de totes les entitats {@code Autor} ordenades pel camp 'nom' de forma ascendent.
     */
    List<Autor> findAllByOrderByNomAsc();
    
}
