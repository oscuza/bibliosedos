package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Autor;
import com.bibliotecasedaos.biblioteca.entity.Llibre;
import com.bibliotecasedaos.biblioteca.error.LlibreNotFoundException;
import com.bibliotecasedaos.biblioteca.repository.AutorRepository;
import com.bibliotecasedaos.biblioteca.repository.LlibreRepository;
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
 * Tests unitaris per a la classe {@link LibreServiceImpl}.
 * 
 * @author David García Rodríguez
 */
@ExtendWith(MockitoExtension.class)
class LibreServiceImplTest {

    @Mock
    private LlibreRepository llibreRepository;

    @Mock
    private AutorRepository autorRepository;

    @InjectMocks
    private LibreServiceImpl llibreService;

    private Autor autor;
    private Llibre llibre1;
    private Llibre llibre2;

    @BeforeEach
    void setUp() {
        autor = Autor.builder()
                .id(1L)
                .nom("Gabriel García Márquez")
                .build();

        llibre1 = Llibre.builder()
                .id(1L)
                .isbn("978-84-376-0494-7")
                .titol("Cien años de soledad")
                .pagines(471)
                .editorial("Cátedra")
                .autor(autor)
                .build();

        llibre2 = Llibre.builder()
                .id(2L)
                .isbn("978-84-376-0495-4")
                .titol("El amor en los tiempos del cólera")
                .pagines(464)
                .editorial("Cátedra")
                .autor(autor)
                .build();
    }

    @Test
    void testFindAllLlibres_Success() {
        // Given
        List<Llibre> llibres = Arrays.asList(llibre1, llibre2);
        when(llibreRepository.findAllByOrderByTitolAsc()).thenReturn(llibres);

        // When
        List<Llibre> result = llibreService.findAllLlibres();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Cien años de soledad", result.get(0).getTitol());
        verify(llibreRepository).findAllByOrderByTitolAsc();
    }

    @Test
    void testSaveLlibre_Success() {
        // Given
        Llibre newLlibre = Llibre.builder()
                .isbn("978-84-376-0496-1")
                .titol("Nuevo Libro")
                .pagines(300)
                .editorial("Nueva Editorial")
                .autor(autor)
                .build();
        Llibre savedLlibre = Llibre.builder()
                .id(3L)
                .isbn("978-84-376-0496-1")
                .titol("Nuevo Libro")
                .pagines(300)
                .editorial("Nueva Editorial")
                .autor(autor)
                .build();
        when(llibreRepository.save(any(Llibre.class))).thenReturn(savedLlibre);

        // When
        Llibre result = llibreService.saveLlibre(newLlibre);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Nuevo Libro", result.getTitol());
        verify(llibreRepository).save(newLlibre);
    }

    @Test
    void testUpdateLlibre_Success() throws LlibreNotFoundException {
        // Given
        Llibre updatedLlibre = Llibre.builder()
                .isbn("978-84-376-0494-7")
                .titol("Cien años de soledad - Edición actualizada")
                .pagines(500)
                .editorial("Cátedra Actualizada")
                .autor(autor)
                .build();
        when(llibreRepository.findById(1L)).thenReturn(Optional.of(llibre1));
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));
        when(llibreRepository.save(any(Llibre.class))).thenReturn(llibre1);

        // When
        Llibre result = llibreService.updateLlibre(1L, updatedLlibre);

        // Then
        assertNotNull(result);
        verify(llibreRepository).findById(1L);
        verify(llibreRepository).save(any(Llibre.class));
    }

    @Test
    void testUpdateLlibre_NotFound_ThrowsException() {
        // Given
        Llibre updatedLlibre = Llibre.builder()
                .titol("Libro Actualizado")
                .build();
        when(llibreRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LlibreNotFoundException.class, () -> {
            llibreService.updateLlibre(999L, updatedLlibre);
        });

        verify(llibreRepository).findById(999L);
        verify(llibreRepository, never()).save(any(Llibre.class));
    }

    @Test
    void testDeleteLlibre_Success() throws LlibreNotFoundException {
        // Given
        when(llibreRepository.findById(1L)).thenReturn(Optional.of(llibre1));
        doNothing().when(llibreRepository).deleteById(1L);

        // When
        llibreService.deleteLlibre(1L);

        // Then
        verify(llibreRepository).findById(1L);
        verify(llibreRepository).deleteById(1L);
    }

    @Test
    void testDeleteLlibre_NotFound_ThrowsException() {
        // Given
        when(llibreRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LlibreNotFoundException.class, () -> {
            llibreService.deleteLlibre(999L);
        });

        verify(llibreRepository).findById(999L);
        verify(llibreRepository, never()).deleteById(anyLong());
    }

    @Test
    void testFindLlibreById_Success() throws LlibreNotFoundException {
        // Given
        when(llibreRepository.findById(1L)).thenReturn(Optional.of(llibre1));

        // When
        Llibre result = llibreService.findLlibreById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Cien años de soledad", result.getTitol());
        verify(llibreRepository).findById(1L);
    }

    @Test
    void testFindLlibreById_NotFound_ThrowsException() {
        // Given
        when(llibreRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LlibreNotFoundException.class, () -> {
            llibreService.findLlibreById(999L);
        });

        verify(llibreRepository).findById(999L);
    }
}















