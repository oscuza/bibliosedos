/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Exemplar;
import com.bibliotecasedaos.biblioteca.entity.Prestec;
import com.bibliotecasedaos.biblioteca.error.ExemplarNotFoundException;
import com.bibliotecasedaos.biblioteca.error.ExemplarReservatException;
import com.bibliotecasedaos.biblioteca.error.PrestecNotFoundException;
import com.bibliotecasedaos.biblioteca.repository.ExemplarRepository;
import com.bibliotecasedaos.biblioteca.repository.PrestecRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementació de la interfície {@link PrestecService}.
 * Conté la lògica de negoci per a la gestió de préstecs, incloent-hi la verificació
 * d'exemplars, el registre de préstecs i la gestió de devolucions.
 * 
 * @author David García Rodríguez
 */
@Service
public class PrestecServiceImpl implements PrestecService{

    private final PrestecRepository prestecRepository;
    private final ExemplarRepository exemplarRepository;

    /**
     * Constructor per injecció de dependències.
     * @param prestecRepository Repositori per accedir a les dades de préstecs.
     * @param exemplarRepository Repositori per accedir a les dades d'exemplars (necessari per a gestionar la disponibilitat).
     */
    @Autowired
    public PrestecServiceImpl(PrestecRepository prestecRepository, ExemplarRepository exemplarRepository) {
        this.prestecRepository = prestecRepository;
        this.exemplarRepository = exemplarRepository;
    }

    /**
     * Si l'ID de l'usuari és proporcionat, es busquen els préstecs actius només per a aquell usuari.
     * Si 'usuariId' és null, es retornen tots els préstecs actius de la biblioteca.
     */
    @Override
    public List<Prestec> findPrestecsActius(Long usuariId) {
        if (usuariId != null) {
            return prestecRepository.findPrestecsActiusByUsuariId(usuariId);
        } else {
            return prestecRepository.findAllPrestecsActius();
        }
    }

    /**
     * Retorna una llista de préstecs (actius i històrics).
     * Si l'ID de l'usuari és null, es retornen tots els préstecs de tota la base de dades.
     */
    @Override
    public List<Prestec> findAllPrestecs(Long usuariId) {
        if (usuariId != null) {
            return prestecRepository.findAllPrestecsByUsuariId(usuariId); 
        } else {
            return prestecRepository.findAll();
        }
    }

    /**
     * Marca un préstec com a retornat.
     * S'utilitza @Transactional per envoltar les operacions de verificació i modificació.
     * @param prestecId L'ID del préstec a retornar.
     * @throws PrestecNotFoundException Si el préstec no existeix.
     */
    @Override
    @Transactional
    public void retornarPrestec(Long prestecId) throws PrestecNotFoundException {
        
        Prestec prestec = prestecRepository.findById(prestecId)
            .orElseThrow(() -> new PrestecNotFoundException("El préstec amb ID " + prestecId + " no s'ha trobat."));
        
        LocalDate dataActual = LocalDate.now();
        int filesModificades = prestecRepository.updateDataDevolucioToCurrentDate(prestecId, dataActual);
        
        if (filesModificades == 0) {
            throw new PrestecNotFoundException("El préstec amb ID " + prestecId + " no es pot actualitzar.");
        }
        
        Exemplar exemplar = prestec.getExemplar(); 
        exemplar.setReservat("lliure");
        exemplarRepository.save(exemplar);
        
    }
    
    
    /**
     * Registra un nou préstec a la base de dades.
     * Aquest mètode verifica la disponibilitat de l'exemplar, actualitza el seu estat a "prestat"
     * i desa l'entitat Prestec.
     * @param prestec L'objecte {@link Prestec} a desar, que conté l'exemplar i l'usuari.
     * @return L'objecte {@link Prestec} desat amb l'ID definit.
     * @throws ExemplarReservatException Si l'exemplar ja es troba en estat "prestat".
     * @throws ExemplarNotFoundException Si l'exemplar referenciat al préstec no existeix.
     */
    @Override
    @Transactional
    public Prestec savePrestec(Prestec prestec) throws ExemplarReservatException, ExemplarNotFoundException {
    
        Long exemplarId = prestec.getExemplar().getId();

        Exemplar exemplarDb = exemplarRepository.findById(exemplarId)
            .orElseThrow(() -> new ExemplarNotFoundException("L'exemplar amb ID " + exemplarId + " no s'ha trobat."));
        
        if ("prestat".equals(exemplarDb.getReservat())) { 
            throw new ExemplarReservatException("L'exemplar amb ID " + exemplarId + " ja està reservat.");
        }
    
        exemplarDb.setReservat("prestat"); 

        if (prestec.getDataPrestec() == null) {
            prestec.setDataPrestec(LocalDate.now());
        }

        exemplarRepository.save(exemplarDb);
        prestec.setExemplar(exemplarDb);
        return prestecRepository.save(prestec);        
    }   
}
