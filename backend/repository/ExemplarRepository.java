/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.repository;

import com.bibliotecasedaos.biblioteca.entity.Exemplar;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositori de dades per a l'entitat Exemplar,extén JpaRepository per proporcionar mètodes CRUD.
 * 
 * @author David García Rodríguez
 */
@Repository
public interface ExemplarRepository extends JpaRepository<Exemplar, Long>{
    
    /**
     * Recupera tots els exemplars de llibres que tenen l'estat de reserva definit com a 'lliure'.
     * @return Una llista d'objectes {@code Exemplar} que estan lliures.
     */
    @Query("SELECT e FROM Exemplar e WHERE e.reservat = 'lliure'")
    List<Exemplar> findExemplarsLliures();
    
    /**
     * Busca exemplars que estan lliures i el títol del llibre associat conté la cadena de cerca.
     * @param titol La cadena de text a buscar dins del títol del llibre.
     * @return Una llista d'exemplars lliures que coincideixen amb el criteri de títol.
     */
    @Query("SELECT e FROM Exemplar e JOIN e.llibre l WHERE e.reservat = 'lliure' AND LOWER(l.titol) LIKE LOWER(CONCAT('%', :titol, '%'))")
    List<Exemplar> findExemplarsLliuresByLlibreTitol(@Param("titol") String titol);
    
    /**
     * Busca exemplars que estan lliures i l'autor del llibre associat conté la cadena de cerca al seu nom.
     * @param nomAutor La cadena de text a buscar dins del nom de l'autor.
     * @return Una llista d'exemplars lliures que coincideixen amb el criteri de l'autor.
     */
    @Query("SELECT e FROM Exemplar e JOIN e.llibre l JOIN l.autor a " +
           "WHERE e.reservat = 'lliure' AND LOWER(a.nom) LIKE LOWER(CONCAT('%', :nomAutor, '%'))")
    List<Exemplar> findExemplarsLliuresByAutorNom(@Param("nomAutor") String nomAutor);
}
