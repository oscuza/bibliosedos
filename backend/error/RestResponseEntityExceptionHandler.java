/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.error;

import com.bibliotecasedaos.biblioteca.error.dto.ErrorMessage;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Gestor centralitzat d'excepcions per a tots els controladors REST de l'aplicació.
 * 
 * @author David García Rodríguez
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler{
    
    /**
     * Maneja les excepcions llançades quan un {@code Usuari} no es pot trobar.
     * @param exception L'excepció {@link UsuariNotFoundException} llançada.
     * @return Una resposta HTTP amb el codi {@code 404 Not Found} i el cos de l'error.
     */
    @ExceptionHandler(UsuariNotFoundException.class)
    public ResponseEntity<ErrorMessage> usuariNotFoundException(UsuariNotFoundException exception) {
        ErrorMessage message = new ErrorMessage(HttpStatus.NOT_FOUND, exception.getMessage());
        return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja les excepcions llançades quan un {@code Autor} no es pot trobar.
     * @param exception L'excepció {@link AutorNotFoundException} llançada.
     * @return Una resposta HTTP amb el codi {@code 404 Not Found} i el cos de l'error.
     */
    @ExceptionHandler(AutorNotFoundException.class)
    public ResponseEntity<ErrorMessage> autorNotFoundException(AutorNotFoundException exception) {
        ErrorMessage message = new ErrorMessage(HttpStatus.NOT_FOUND, exception.getMessage());
        return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Maneja les excepcions llançades quan un {@code Llibre} no es pot trobar.
     * @param exception L'excepció {@link LlibreNotFoundException} llançada.
     * @return Una resposta HTTP amb el codi {@code 404 Not Found} i el cos de l'error.
     */
    @ExceptionHandler(LlibreNotFoundException.class)
    public ResponseEntity<ErrorMessage> llibreNotFoundException(LlibreNotFoundException exception) {
        ErrorMessage message = new ErrorMessage(HttpStatus.NOT_FOUND, exception.getMessage());
        return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Maneja les excepcions llançades quan un {@code Exemplar} no es pot trobar.
     * @param exception L'excepció {@link ExemplarNotFoundException} llançada.
     * @return Una resposta HTTP amb el codi {@code 404 Not Found} i el cos de l'error.
     */
    @ExceptionHandler(ExemplarNotFoundException.class)
    public ResponseEntity<ErrorMessage> exemplarNotFoundException(ExemplarNotFoundException exception) {
        ErrorMessage message = new ErrorMessage(HttpStatus.NOT_FOUND, exception.getMessage());
        return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(ExemplarReservatException.class)
    public ResponseEntity<ErrorMessage> exemplarReservatException(ExemplarReservatException exception) {
        ErrorMessage message = new ErrorMessage(HttpStatus.CONFLICT, exception.getMessage());
        return new ResponseEntity<>(message, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(PrestecNotFoundException.class)
    public ResponseEntity<ErrorMessage> prestecNotFoundException(PrestecNotFoundException exception) {
        ErrorMessage message = new ErrorMessage(HttpStatus.NOT_FOUND, exception.getMessage());
        return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Sobreescriu el mètode per manejar errors de validació d'arguments (per exemple, amb {@code @Valid}
     * en el controlador).
     * <p>Recull tots els errors de camp i els retorna en un mapa amb el codi {@code 400 Bad Request}.</p>
     * * {@inheritDoc}
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String,Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->{
            errors.put(error.getField(), error.getDefaultMessage());
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    /**
     * Maneja l'excepció de negoci llançada quan un camp únic (com el Nick) ja existeix.
     * @param ex L'excepció {@link NickAlreadyExistsException} llançada.
     * @return Una resposta HTTP amb el codi {@code 409 Conflict} i el missatge d'error.
     */
    @ExceptionHandler(NickAlreadyExistsException.class)
    public ResponseEntity<ErrorMessage> handleNickAlreadyExistsException(NickAlreadyExistsException ex) {
        ErrorMessage message = new ErrorMessage(HttpStatus.CONFLICT, ex.getMessage());
        return new ResponseEntity<>(message, HttpStatus.CONFLICT);
    }
    
    /**
     * Maneja les fallades de validació de JPA/Hibernate (Ex: {@code @Size}, {@code @NotNull})
     * que ocorren a la capa de persistència o directament.
     * @param ex L'excepció {@code ConstraintViolationException} llançada.
     * @return Una resposta HTTP amb el codi {@code 400 Bad Request} que conté un mapa d'errors.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(
        jakarta.validation.ConstraintViolationException ex) {
        
        Map<String, String> errors = new HashMap<>();
        
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Maneja els errors d'integritat de la base de dades (com violacions de clau única,
     * clau forana o restriccions NOT NULL a la capa de persistència).
     * <p>Retorna un missatge genèric de conflicte amb el codi {@code 409 Conflict}.</p>
     * * @param ex L'excepció {@link DataIntegrityViolationException} llançada per Spring/JPA.
     * @return Una resposta HTTP amb el codi {@code 409 Conflict} i el cos de l'error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessage> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        
        String errorMessage = "S'ha violat una restricció de dades (clau duplicada o valor nul). ";
        
        if (ex.getCause() != null && ex.getCause().getMessage().contains("llave duplicada")) {
            errorMessage += "Detall: " + ex.getCause().getMessage();
        } else {
            errorMessage += "Si us plau, revisa si el Nick, NIF o Email ja existeixen.";
        }

        ErrorMessage message = new ErrorMessage(HttpStatus.CONFLICT, errorMessage);
        return new ResponseEntity<>(message, HttpStatus.CONFLICT);
    }
    
    /**
     * Maneja les excepcions d'autorització denegada llançades per Spring Security.
     * <p>Aquest mètode s'activa quan una sol·licitud és rebutjada per una regla
     * {@code @PreAuthorize} o un altre mecanisme d'autorització, i retorna un
     * codi d'estat HTTP {@code 403 Forbidden}.</p>
     * @param ex L'excepció {@link AuthorizationDeniedException} llançada.
     * @return Una resposta HTTP amb el codi {@code 403 Forbidden} i el cos de l'error.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        String detailedMessage = "Accés Denegat: No tens els permisos necessaris per realitzar aquesta acció.";
        ErrorMessage message = new ErrorMessage(HttpStatus.FORBIDDEN, detailedMessage);
        return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
    }
}
