package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.entity.Autor;
import com.bibliotecasedaos.biblioteca.error.AutorNotFoundException;
import com.bibliotecasedaos.biblioteca.repository.AutorRepository;
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
 * Tests unitaris per a la classe {@link AutorServiceImpl}.
 * 
 * @author David García Rodríguez
 */
@ExtendWith(MockitoExtension.class)
class AutorServiceImplTest {

    @Mock
    private AutorRepository autorRepository;

    @InjectMocks
    private AutorServiceImpl autorService;

    private Autor autor1;
    private Autor autor2;

    @BeforeEach
    void setUp() {
        autor1 = Autor.builder()
                .id(1L)
                .nom("Gabriel García Márquez")
                .build();

        autor2 = Autor.builder()
                .id(2L)
                .nom("Isabel Allende")
                .build();
    }

    @Test
    void testFindAllAutors_Success() {
        // Given
        List<Autor> autors = Arrays.asList(autor1, autor2);
        when(autorRepository.findAllByOrderByNomAsc()).thenReturn(autors);

        // When
        List<Autor> result = autorService.findAllAutors();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Gabriel García Márquez", result.get(0).getNom());
        verify(autorRepository).findAllByOrderByNomAsc();
    }

    @Test
    void testFindAutorById_Success() throws AutorNotFoundException {
        // Given
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autor1));

        // When
        Autor result = autorService.findAutorById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Gabriel García Márquez", result.getNom());
        verify(autorRepository).findById(1L);
    }

    @Test
    void testFindAutorById_NotFound_ThrowsException() {
        // Given
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AutorNotFoundException.class, () -> {
            autorService.findAutorById(999L);
        });

        verify(autorRepository).findById(999L);
    }

    @Test
    void testSaveAutor_Success() {
        // Given
        Autor newAutor = Autor.builder()
                .nom("Nuevo Autor")
                .build();
        Autor savedAutor = Autor.builder()
                .id(3L)
                .nom("Nuevo Autor")
                .build();
        when(autorRepository.save(any(Autor.class))).thenReturn(savedAutor);

        // When
        Autor result = autorService.saveAutor(newAutor);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Nuevo Autor", result.getNom());
        verify(autorRepository).save(newAutor);
    }

    @Test
    void testDeleteAutor_Success() throws AutorNotFoundException {
        // Given
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autor1));
        doNothing().when(autorRepository).deleteById(1L);

        // When
        autorService.deleteAutor(1L);

        // Then
        verify(autorRepository).findById(1L);
        verify(autorRepository).deleteById(1L);
    }

    @Test
    void testDeleteAutor_NotFound_ThrowsException() {
        // Given
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AutorNotFoundException.class, () -> {
            autorService.deleteAutor(999L);
        });

        verify(autorRepository).findById(999L);
        verify(autorRepository, never()).deleteById(anyLong());
    }
}










