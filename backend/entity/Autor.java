/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entitat que representa un Autor a la base de dades.
 * 
 * @author David García Rodríguez
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "autors", uniqueConstraints = @UniqueConstraint(name = "nom_unique", columnNames = "nom"))
public class Autor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "autors_seq")
    @SequenceGenerator(name = "autors_seq", sequenceName = "autors_id_seq", allocationSize = 1)
    private Long id;
    
    @Column(name = "nom", nullable = false)
    @NotBlank(message = "El nom de l'autor no pot ser buit.")
    private String nom;   
}
