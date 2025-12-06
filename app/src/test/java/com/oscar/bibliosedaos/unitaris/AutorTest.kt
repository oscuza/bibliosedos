package com.oscar.bibliosedaos.unitaris

import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaris per a la gestió d'autors en BookViewModel.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la funcionalitat relacionada amb la gestió d'autors
 * en el BookViewModel utilitzant APIs falses (FakeBookApiSuccess).
 * 
 * **Cobertura:**
 * - Càrrega d'autors
 * - Creació de nous autors
 * - Eliminació d'autors
 * - Gestió d'estats (isLoading, error, isCreating, isDeleting)
 * 
 *
 * @author Oscar
 * @since 1.0
 * @see BookViewModel
 * @see FakeBookApiSuccess
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutorTest : BaseBookViewModelTest() {

    /**
     * Test: Càrrega d'autors amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar autors correctament des de l'API falsa
     * - Que l'estat `isLoading` es gestiona correctament
     * - Que la llista d'autors es carrega correctament
     * - Que la llista conté l'autor inicial "Gabriel García Márquez"
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que retorna 1 autor inicial
     * 
     * **Resultats esperats:**
     * - isLoading == false
     * - autors.isNotEmpty() == true
     * - autors conté "Gabriel García Márquez"
     * - error == null
     */
    @Test
    fun loadAutors_exitos_retornaLlista() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadAutors()
        advanceUntilIdle()

        val state = vm.autorsState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir autors", state.autors.isNotEmpty())
        assertTrue(
            "Hauria de contenir l'autor inicial",
            state.autors.any { it.nom == "Gabriel García Márquez" }
        )
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Creació d'autor amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot crear un nou autor correctament
     * - Que l'estat `isCreating` es gestiona correctament
     * - Que el nou autor s'afegeix a la llista després de crear-lo
     * - Que no hi ha errors en la operació
     * - Que després de crear, la llista conté el nou autor i l'autor inicial
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que gestiona la creació internament
     * - Inicialment hi ha 1 autor
     * - Es crea un nou autor "Isabel Allende"
     * 
     * **Resultats esperats:**
     * - isCreating == false
     * - autors.size == 2 (1 inicial + 1 nou)
     * - autors conté "Isabel Allende"
     * - autors conté "Gabriel García Márquez"
     * - error == null
     */
    @Test
    fun createAutor_exitos_afegeixALlista() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadAutors()
        advanceUntilIdle()

        val initialState = vm.autorsState.value
        assertEquals("Inicialment hauria de tenir 1 autor", 1, initialState.autors.size)

        vm.createAutor("Isabel Allende")
        advanceUntilIdle()

        vm.loadAutors()
        advanceUntilIdle()

        val state = vm.autorsState.value
        assertFalse("No hauria d'estar creant", state.isCreating)
        assertEquals("Hauria de tenir 2 autors", 2, state.autors.size)
        assertTrue(
            "Hauria de contenir 'Isabel Allende'",
            state.autors.any { it.nom == "Isabel Allende" }
        )
        assertTrue(
            "Hauria de contenir 'Gabriel García Márquez'",
            state.autors.any { it.nom == "Gabriel García Márquez" }
        )
    }

    /**
     * Test: Eliminació d'autor amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot eliminar un autor existent correctament
     * - Que l'estat `isDeleting` es gestiona correctament
     * - Que l'autor s'elimina de la llista
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que gestiona l'eliminació internament
     * - Inicialment hi ha 1 autor amb id=1L
     * - S'elimina l'autor amb id=1L
     * 
     * **Resultats esperats:**
     * - isDeleting == null
     * - autors.size == 0 (autor eliminat)
     * - error == null
     */
    @Test
    fun deleteAutor_exitos_eliminaAutor() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadAutors()
        advanceUntilIdle()

        vm.deleteAutor(1L)
        advanceUntilIdle()

        val state = vm.autorsState.value
        assertNull("No hauria d'estar eliminant", state.isDeleting)
        assertEquals("Hauria de tenir 0 autors", 0, state.autors.size)
    }
}


