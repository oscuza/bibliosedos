package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Exemplar;
import com.bibliotecasedaos.biblioteca.entity.Llibre;
import com.bibliotecasedaos.biblioteca.error.ExemplarNotFoundException;
import com.bibliotecasedaos.biblioteca.repository.ExemplarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaris per a la classe {@link ExemplarServiceImpl}.
 * 
 * @author David García Rodríguez
 */
@ExtendWith(MockitoExtension.class)
class ExemplarServiceImplTest {

    @Mock
    private ExemplarRepository exemplarRepository;

    @InjectMocks
    private ExemplarServiceImpl exemplarService;

    private Llibre llibre;
    private Exemplar exemplar1;
    private Exemplar exemplar2;

    @BeforeEach
    void setUp() {
        llibre = Llibre.builder()
                .id(1L)
                .isbn("978-84-376-0494-7")
                .titol("Cien años de soledad")
                .pagines(471)
                .editorial("Cátedra")
                .build();

        exemplar1 = Exemplar.builder()
                .id(1L)
                .lloc("Estantería A-1")
                .reservat("lliure")
                .llibre(llibre)
                .build();

        exemplar2 = Exemplar.builder()
                .id(2L)
                .lloc("Estantería A-2")
                .reservat("prestat")
                .llibre(llibre)
                .build();
    }

    @Test
    void testFindAllExemplars_Success() {
        // Given
        List<Exemplar> exemplars = Arrays.asList(exemplar1, exemplar2);
        when(exemplarRepository.findAll()).thenReturn(exemplars);

        // When
        List<Exemplar> result = exemplarService.findAllExemplars();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(exemplarRepository).findAll();
    }

    @Test
    void testSaveExemplar_Success() {
        // Given
        Exemplar newExemplar = Exemplar.builder()
                .lloc("Estantería B-1")
                .reservat("lliure")
                .llibre(llibre)
                .build();
        Exemplar savedExemplar = Exemplar.builder()
                .id(3L)
                .lloc("Estantería B-1")
                .reservat("lliure")
                .llibre(llibre)
                .build();
        when(exemplarRepository.save(any(Exemplar.class))).thenReturn(savedExemplar);

        // When
        Exemplar result = exemplarService.saveExemplar(newExemplar);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Estantería B-1", result.getLloc());
        verify(exemplarRepository).save(newExemplar);
    }

    @Test
    void testUpdateExemplar_Success() throws ExemplarNotFoundException {
        // Given
        Exemplar updatedExemplar = Exemplar.builder()
                .lloc("Estantería A-1 Actualizada")
                .reservat("prestat")
                .build();
        when(exemplarRepository.findById(1L)).thenReturn(Optional.of(exemplar1));
        when(exemplarRepository.save(any(Exemplar.class))).thenReturn(exemplar1);

        // When
        Exemplar result = exemplarService.updateExemplar(1L, updatedExemplar);

        // Then
        assertNotNull(result);
        verify(exemplarRepository).findById(1L);
        verify(exemplarRepository).save(any(Exemplar.class));
    }

    @Test
    void testUpdateExemplar_NotFound_ThrowsException() {
        // Given
        Exemplar updatedExemplar = Exemplar.builder()
                .lloc("Nueva Ubicación")
                .build();
        when(exemplarRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ExemplarNotFoundException.class, () -> {
            exemplarService.updateExemplar(999L, updatedExemplar);
        });

        verify(exemplarRepository).findById(999L);
        verify(exemplarRepository, never()).save(any(Exemplar.class));
    }

    @Test
    void testDeleteExemplar_Success() throws ExemplarNotFoundException {
        // Given
        when(exemplarRepository.findById(1L)).thenReturn(Optional.of(exemplar1));
        doNothing().when(exemplarRepository).deleteById(1L);

        // When
        exemplarService.deleteExemplar(1L);

        // Then
        verify(exemplarRepository).findById(1L);
        verify(exemplarRepository).deleteById(1L);
    }

    @Test
    void testDeleteExemplar_NotFound_ThrowsException() {
        // Given
        when(exemplarRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ExemplarNotFoundException.class, () -> {
            exemplarService.deleteExemplar(999L);
        });

        verify(exemplarRepository).findById(999L);
        verify(exemplarRepository, never()).deleteById(anyLong());
    }

    @Test
    void testFindExemplarById_Success() throws ExemplarNotFoundException {
        // Given
        when(exemplarRepository.findById(1L)).thenReturn(Optional.of(exemplar1));

        // When
        Exemplar result = exemplarService.findExemplarById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Estantería A-1", result.getLloc());
        verify(exemplarRepository).findById(1L);
    }

    @Test
    void testFindExemplarsLliuresByTitolOrAutor_ByTitol() {
        // Given
        List<Exemplar> exemplarsLliures = Arrays.asList(exemplar1);
        when(exemplarRepository.findExemplarsLliuresByLlibreTitol("Cien años")).thenReturn(exemplarsLliures);

        // When
        List<Exemplar> result = exemplarService.findExemplarsLliuresByTitolOrAutor("Cien años", null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(exemplarRepository).findExemplarsLliuresByLlibreTitol("Cien años");
        verify(exemplarRepository, never()).findExemplarsLliuresByAutorNom(anyString());
    }

    @Test
    void testFindExemplarsLliuresByTitolOrAutor_ByAutor() {
        // Given
        List<Exemplar> exemplarsLliures = Arrays.asList(exemplar1);
        when(exemplarRepository.findExemplarsLliuresByAutorNom("García")).thenReturn(exemplarsLliures);

        // When
        List<Exemplar> result = exemplarService.findExemplarsLliuresByTitolOrAutor(null, "García");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(exemplarRepository).findExemplarsLliuresByAutorNom("García");
        verify(exemplarRepository, never()).findExemplarsLliuresByLlibreTitol(anyString());
    }

    @Test
    void testFindExemplarsLliuresByTitolOrAutor_AllLliures() {
        // Given
        List<Exemplar> exemplarsLliures = Arrays.asList(exemplar1);
        when(exemplarRepository.findExemplarsLliures()).thenReturn(exemplarsLliures);

        // When
        List<Exemplar> result = exemplarService.findExemplarsLliuresByTitolOrAutor(null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(exemplarRepository).findExemplarsLliures();
        verify(exemplarRepository, never()).findExemplarsLliuresByLlibreTitol(anyString());
        verify(exemplarRepository, never()).findExemplarsLliuresByAutorNom(anyString());
    }
}















