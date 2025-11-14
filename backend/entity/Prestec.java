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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entitat que representa un Prestec a la base de dades.
 * 
 * @author David García Rodríguez
 */
@Entity
@Table(name = "prestecs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Prestec {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prestecs_seq")
    @SequenceGenerator(name = "prestecs_seq", sequenceName = "prestecs_id_seq", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate dataPrestec;
    private LocalDate dataDevolucio;
    
    @ManyToOne
    @JoinColumn(name = "usuari_id", referencedColumnName = "id", nullable = false)
    private Usuari usuari;
    
    @ManyToOne
    @JoinColumn(name = "exemplar_id", referencedColumnName = "id", nullable = false)
    private Exemplar exemplar;
}
