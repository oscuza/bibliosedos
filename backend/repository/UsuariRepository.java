/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.repository;

import com.bibliotecasedaos.biblioteca.entity.Usuari;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositori de dades per a la gestió de la persistència de l'entitat {@code Usuari}.
 * Estén {@link JpaRepository} per proporcionar mètodes CRUD
 * 
 * @author David García Rodríguez
 */
@Repository
public interface UsuariRepository extends JpaRepository<Usuari, Long>{
    
    /**
     * Comprova l'existència d'un usuari basant-se en el seu nom d'usuari (nick).
     *
     * @param nick El nom d'usuari a comprovar.
     * @return {@code true} si existeix un usuari amb el nick donat, {@code false} altrament.
     */
    boolean existsByNick(String nick);
    
    /**
     * Cerca un usuari pel seu nom d'usuari (nick) utilitzant una consulta JPQL explícita.
     *
     * @param nick El nom d'usuari a cercar.
     * @return Un {@link Optional} que conté l'usuari si es troba, o un {@code Optional.empty()} altrament.
     */
    @Query("SELECT u FROM Usuari u WHERE u.nick = :nick")
    Optional<Usuari> findUsuariByNickWithJPQL(String nick);
    
    
    /**
     * Cerca un usuari pel seu Número d'Identificació Fiscal (NIF)..
     *
     * @param nif El NIF a cercar.
     * @return Un {@link Optional} que conté l'usuari si es troba, o un {@code Optional.empty()} altrament.
     */
    Optional<Usuari> findByNif(String nif);
    
    /**
     * Cerca un usuari pel seu Número d'Identificació Fiscal (NIF) utilitzant una consulta JPQL explícita.
     *
     * @param nif El NIF a cercar.
     * @return Un {@link Optional} que conté l'usuari si es troba, o un {@code Optional.empty()} altrament.
     */
    @Query("SELECT u FROM Usuari u Where u.nif = :nif")
    Optional<Usuari> findUsuariByNifWithJPQL(@Param ("nif") String nif);
    
    //añadido de prueba para los permisos por id
    public Usuari findByNick(String authenticatedNick);
}
