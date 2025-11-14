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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entitat que representa un Llibre a la base de dades.
 * 
 * @author David García Rodríguez
 */
@Entity
@Table(name = "llibres")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Llibre {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "llibres_seq")
    @SequenceGenerator(name = "llibres_seq", sequenceName = "llibres_id_seq", allocationSize = 1)
    private Long id;
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Per favor afegeix un isbn")
    private String isbn;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix un titol")
    private String titol;
    @Column(nullable = false) 
    @NotNull(message = "El nombre de pàgines no pot ser null.")
    @Min(value = 1, message = "El nombre de pàgines ha de ser com a mínim 1.")
    private int pagines;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix nom d'editorial")
    private String editorial;
    
    @ManyToOne
    @JoinColumn(name = "autor_id", referencedColumnName = "id")
    private Autor autor;
}
