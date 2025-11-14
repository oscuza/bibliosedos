/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.repository;

import com.bibliotecasedaos.biblioteca.entity.Prestec;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * * Repositori de dades per a l'entitat {@link Prestec}.
 * Proporciona operacions CRUD bàsiques a través de JpaRepository i mètodes de consulta personalitzats.
 * 
 * @author David García Rodríguez
 */
@Repository
public interface PrestecRepository extends JpaRepository<Prestec,Long> {
    
    /**
     * Recupera una llista de Préstecs que compleixen dues condicions: id de l'usuari i devolució null
     * @param usuariId L'ID de l'Usuari.
     * @return Una llista de Préstecs actius de l'usuari.
     */
    @Query("SELECT p FROM Prestec p WHERE p.usuari.id = :usuariId AND p.dataDevolucio IS NULL")
    List<Prestec> findPrestecsActiusByUsuariId(@Param("usuariId") Long usuariId);
    
    /**
     * Recupera una llista de tots els Préstecs que corresponen a l'usuari identificat per 'usuariId'.
     * @param usuariId L'ID de l'Usuari.
     * @return Una llista de tots els Préstecs de l'usuari.
     */
    @Query("SELECT p FROM Prestec p WHERE p.usuari.id = :usuariId")
    List<Prestec> findAllPrestecsByUsuariId(@Param("usuariId") Long usuariId);
    
    /**
     * Recupera una llista de TOTS els Préstecs actius (dataDevolucio IS NULL),
     * @return Una llista  préstecs.
     */
    @Query("SELECT p FROM Prestec p WHERE p.dataDevolucio IS NULL")
    List<Prestec> findAllPrestecsActius(); 
    
    /**
     * Actualitza el camp 'dataDevolucio' d'un Préstec específic amb la data i hora actual.
     * @param prestecId L'ID del Préstec a marcar com a retornat.
     * @return El nombre de files modificades (hauria de ser 1).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Prestec p SET p.dataDevolucio = :dataDevolucio WHERE p.id = :prestecId") 
    int updateDataDevolucioToCurrentDate(@Param("prestecId") Long prestecId, 
                                         @Param("dataDevolucio") LocalDate dataDevolucio);
    //@Query("UPDATE Prestec p SET p.dataDevolucio = CURRENT_TIMESTAMP() WHERE p.id = :prestecId") 
    //int updateDataDevolucioToCurrentDate(@Param("prestecId") Long prestecId);
    
}
