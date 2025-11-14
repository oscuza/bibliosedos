/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Llibre;
import com.bibliotecasedaos.biblioteca.error.LlibreNotFoundException;
import java.util.List;
import java.util.Optional;

/**
 * Interfície de servei per a la gestió dels llibres a l'aplicació, defineix les operacions bàsiques per a la cerca i el manteniment.
 * 
 * @author David García Rodríguez
 */
public interface LlibreService {
    
    List<Llibre> findAllLlibres();
    
    Llibre saveLlibre(Llibre llibre);
    Llibre updateLlibre(Long id, Llibre llibre) throws LlibreNotFoundException;
    void deleteLlibre(Long id) throws LlibreNotFoundException;   
    Llibre findLlibreById(Long id) throws LlibreNotFoundException;
}
