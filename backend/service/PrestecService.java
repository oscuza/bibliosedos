/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Prestec;
import com.bibliotecasedaos.biblioteca.error.ExemplarNotFoundException;
import com.bibliotecasedaos.biblioteca.error.ExemplarReservatException;
import com.bibliotecasedaos.biblioteca.error.PrestecNotFoundException;
import java.util.List;

/**
 * Interfície de servei per gestionar la lògica de negoci relacionada amb els préstecs (Prestec).
 * 
 * @author David García Rodríguez
 */
public interface PrestecService {
    
    /**
     * Troba tots els préstecs actius (dataDevolucio és NULL) d'un usuari o de tots.
     * @param usuariId L'ID de l'usuari.
     * @return Llista de préstecs actius.
     */
    List<Prestec> findPrestecsActius(Long usuariId);

    /**
     * Troba tots els préstecs (actius i històrics) d'un usuari.
     * @param usuariId L'ID de l'usuari.
     * @return Llista de tots els préstecs.
     */
    List<Prestec> findAllPrestecs(Long usuariId);

    /**
     * Marca un préstec com a retornat actualitzant la dataDevolucio amb la data actual.
     * @param prestecId L'ID del préstec a retornar.
     * @throws PrestecNotFoundException Si el préstec no existeix.
     */
    void retornarPrestec(Long prestecId) throws PrestecNotFoundException;
    
    /**
     * Guarda un nou préstec a la base de dades, un exemplar i usuari específics.
     * @param prestec L'objecte {@link Prestec} que es vol desar (ha de contenir l'usuari i l'exemplar).
     * @return L'objecte {@link Prestec} desat amb l'ID generat.
     * @throws ExemplarReservatException Si l'exemplar que es vol prestar ja està reservat o no està disponible.
     * @throws ExemplarNotFoundException Si l'exemplar o l'usuari referenciats no es troben a la base de dades.
     */
    Prestec savePrestec(Prestec prestec) throws ExemplarReservatException, ExemplarNotFoundException;
    //Prestec savePrestec(Prestec prestec);
}
