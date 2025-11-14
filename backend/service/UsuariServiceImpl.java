/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Usuari;
import com.bibliotecasedaos.biblioteca.error.UsuariNotFoundException;
import com.bibliotecasedaos.biblioteca.repository.UsuariRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementació del servei de lògica de negoci per a la gestió d'usuaris.
 * Aquesta classe actua com a pont entre el controlador i la capa de persistència
 * (Repositori), contenint la lògica per a la validació, cerca i modificació de dades d'usuaris.
 * 
 * @author David García Rodríguez
 */
@Service
public class UsuariServiceImpl implements UsuariService {

    @Autowired
    UsuariRepository usuariRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Recupera una llista de tots els usuaris emmagatzemats a la base de dades.
     *
     * @return Una llista de totes les entitats {@code Usuari}.
     */
    @Override
    public List<Usuari> findAllUsuaris() {
        return usuariRepository.findAll();
    }

    /**
     * Guarda una nova entitat d'usuari a la base de dades.
     * Utilitzat principalment per a la creació d'un nou usuari i actualitzacions. 
     *
     * @param usuari L'entitat {@code Usuari} a guardar.
     * @return L'entitat {@code Usuari} guardada amb possibles modificacions (ex. ID assignat).
     */
    @Override
    public Usuari saveUsuari(Usuari usuari) {
        return usuariRepository.save(usuari);
    }

    /**
     * Actualitza els camps d'un usuari existent basant-se en l'ID proporcionat.
     * Només s'actualitzen els camps que no són nuls o buits en l'entitat {@code usuari}
     * de la petició.
     *
     * @param id L'identificador de l'usuari a actualitzar.
     * @param usuari L'entitat {@code Usuari} amb els nous valors a aplicar.
     * @return L'entitat {@code Usuari} amb les dades actualitzades.
     * @throws UsuariNotFoundException Si no es troba cap usuari amb l'ID donat.
     */
    @Override
    public Usuari updateUsuari(Long id, Usuari usuari) throws UsuariNotFoundException{
        Usuari usuariDb = usuariRepository.findById(id)
            .orElseThrow(() -> new UsuariNotFoundException("Usuari amb ID " + id + " no trobat."));
        
        if (Integer.valueOf(usuari.getRol()) != null && usuari.getRol() >= 0 && usuari.getRol() <= 1) {
            //usuariDb.setRol(usuari.getRol());
        }
        if (Objects.nonNull(usuari.getCarrer()) && !usuari.getCarrer().isBlank()) {
            usuariDb.setCarrer(usuari.getCarrer());
        }
        if (Objects.nonNull(usuari.getCognom1()) && !usuari.getCognom1().isBlank()) {
            usuariDb.setCognom1(usuari.getCognom1());
        }            
        if (Objects.nonNull(usuari.getCognom2()) && !"".equalsIgnoreCase(usuari.getCognom2())) {
            usuariDb.setCognom2(usuari.getCognom2());
        }
        if (Objects.nonNull(usuari.getCp()) && !usuari.getCp().isBlank()) {
            usuariDb.setCp(usuari.getCp());
        }
        if (Objects.nonNull(usuari.getEmail()) && !usuari.getEmail().isBlank()) {
            usuariDb.setEmail(usuari.getEmail());
        }
        if (Objects.nonNull(usuari.getLocalitat()) && !usuari.getLocalitat().isBlank()) {
            usuariDb.setLocalitat(usuari.getLocalitat());
        }
        if (Objects.nonNull(usuari.getNick()) && !usuari.getNick().isBlank()) {
            usuariDb.setNick(usuari.getNick());
        }
        if (Objects.nonNull(usuari.getNif()) && !usuari.getNif().isBlank()) {
            usuariDb.setNif(usuari.getNif());
        }
        if (Objects.nonNull(usuari.getNom()) && !usuari.getNom().isBlank()) {
            usuariDb.setNom(usuari.getNom());
        }
        if (Objects.nonNull(usuari.getPassword()) && !usuari.getPassword().isBlank()) {
            String rawPassword = usuari.getPassword().trim();
            String encodedPassword = passwordEncoder.encode(rawPassword);
            usuariDb.setPassword(encodedPassword);   
        }
        
        if (Objects.nonNull(usuari.getProvincia()) && !usuari.getProvincia().isBlank()) {
            usuariDb.setProvincia(usuari.getProvincia());
        }
        if (Objects.nonNull(usuari.getTlf()) && !usuari.getTlf().isBlank()) {
            usuariDb.setTlf(usuari.getTlf());
        }
        
        return usuariRepository.save(usuariDb);
        
    }

    /**
     * Elimina un usuari de la base de dades mitjançant el seu identificador.
     *
     * @param id L'identificador de l'usuari a eliminar.
     */
    @Override
    public void deleteUsuari(Long id) {
        usuariRepository.deleteById(id);
    }

    /**
     * Cerca un usuari pel seu nom d'usuari (nick).
     *
     * @param nick El nom d'usuari a cercar.
     * @return Un {@link Optional} que conté l'usuari trobat.
     * @throws UsuariNotFoundException Si l'usuari no es troba pel nick.
     */
    @Override
    public Optional<Usuari> findUsuariByNameWithJPQL(String nick) throws UsuariNotFoundException {
        Optional<Usuari> usuari =  usuariRepository.findUsuariByNickWithJPQL(nick);
        if (!usuari.isPresent()) {
            throw new UsuariNotFoundException("Usuari no trobat per nick.");
        }
        return usuari;
    }

    @Override
    public Optional<Usuari> findByNif(String nif) {
        return usuariRepository.findByNif(nif);
    }

    /**
     * Cerca un usuari pel seu identificador (ID).
     *
     * @param id L'identificador de l'usuari a cercar.
     * @return L'entitat {@code Usuari} trobada.
     * @throws UsuariNotFoundException Si no es troba cap usuari amb l'ID donat.
     */
    @Override
    public Usuari findUsuariById(Long id) throws UsuariNotFoundException{
        Optional<Usuari> usuari = usuariRepository.findById(id);
        if (!usuari.isPresent()) {
            throw new UsuariNotFoundException("Usuari no trobat.");
        }
        return usuari.get();
    }

    @Override
    public Optional<Usuari> findUsuariByNifWithJPQL(String nif) {
        return usuariRepository.findUsuariByNifWithJPQL(nif);
    }
    
    /**
     * Comprova si l'usuari actualment autenticat és el propietari del recurs sol·licitat.
     * @param requestedId L'identificador (ID) del recurs o entitat que s'està intentant accedir.
     * @param principal L'objecte principal de l'autenticació de Spring Security, que pot ser un {@code String}
     * (el nom d'usuari) o un objecte {@code Usuari} (l'entitat completa).
     * @return {@code true} si l'usuari autenticat és l'administrador o si l'ID de l'usuari
     * autenticat coincideix amb el {@code requestedId}; {@code false} en cas contrari (incloent-hi
     * usuaris anònims o no trobats).
     */
    public boolean isResourceOwner(Long requestedId, Object principal) {
    
        String authenticatedNick = null;

        if (principal instanceof String) {
            authenticatedNick = (String) principal;
        } else if (principal instanceof Usuari) { 
            authenticatedNick = ((Usuari) principal).getNick();
        }
    
        if (authenticatedNick == null || authenticatedNick.isBlank() || "ANONYMOUS".equals(authenticatedNick)) {
            return false;
        }

        Optional<Usuari> usuariOptional = usuariRepository.findUsuariByNickWithJPQL(authenticatedNick);
    
        if (usuariOptional.isEmpty()) {
            return false;
        }

        Long currentUserId = usuariOptional.get().getId();
    
        return Objects.equals(currentUserId, requestedId);
    }
    
}
