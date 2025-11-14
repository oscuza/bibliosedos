/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.repository;

import com.bibliotecasedaos.biblioteca.entity.Llibre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori de dades per a l'entitat Llibre, que proporcionar mètodes de gestió de dades (CRUD).
 * 
 * @author David García Rodríguez
 */
@Repository
public interface LlibreRepository extends JpaRepository<Llibre,Long>{
    
    /**
     * Recupera una llista de tots els llibres presents a la base de dades.
     * @return Una llista d'objectes {@code Llibre} ordenats per títol ascendent.
     */
    List<Llibre> findAllByOrderByTitolAsc();
}
