package com.oscar.bibliosedaos.unitaris

import com.oscar.bibliosedaos.data.models.Exemplar
import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaris per a la gestió d'exemplars en BookViewModel.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la funcionalitat relacionada amb la gestió d'exemplars
 * en el BookViewModel utilitzant APIs falses (FakeBookApiSuccess).
 * 
 * **Cobertura:**
 * - ✅ Càrrega d'exemplars
 * - ✅ Cerca d'exemplars lliures
 * - ✅ Creació de nous exemplars
 * - ✅ Actualització d'exemplars existents
 * - ✅ Eliminació d'exemplars
 * - ✅ Gestió d'estats (isLoading, error, isCreating, isUpdating, isDeleting)
 * 
 *
 * @author Oscar
 * @since 1.0
 * @see BookViewModel
 * @see FakeBookApiSuccess
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExemplarTest : BaseBookViewModelTest() {

    /**
     * Test: Càrrega d'exemplars amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar exemplars correctament des de l'API falsa
     * - Que l'estat `isLoading` es gestiona correctament
     * - Que la llista d'exemplars es carrega correctament
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que retorna 1 exemplar inicial
     * 
     * **Resultats esperats:**
     * - isLoading == false
     * - exemplars.isNotEmpty() == true
     * - error == null
     */
    @Test
    fun loadExemplars_exitos_retornaLlista() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadExemplars()
        advanceUntilIdle()

        val state = vm.exemplarsState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir exemplars", state.exemplars.isNotEmpty())
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Cerca d'exemplars lliures amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot cercar exemplars lliures correctament
     * - Que l'estat `isSearching` es gestiona correctament
     * - Que tots els resultats de la cerca són exemplars lliures
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que filtra exemplars amb reservat="lliure"
     * - Es busca sense paràmetres (null, null)
     * 
     * **Resultats esperats:**
     * - isSearching == false
     * - searchResults != null
     * - Tots els resultats tenen reservat == "lliure"
     * - error == null
     */
    @Test
    fun searchExemplarsLliures_exitos_retornaExemplarsLliures() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.searchExemplarsLliures(null, null)
        advanceUntilIdle()

        val state = vm.exemplarsState.value
        assertFalse("No hauria d'estar cercant", state.isSearching)
        assertNotNull("Hauria de tenir resultats", state.searchResults)
        assertTrue(
            "Tots els resultats haurien d'estar lliures",
            state.searchResults!!.all { it.reservat == "lliure" }
        )
    }

    /**
     * Test: Creació d'exemplar amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot crear un nou exemplar correctament
     * - Que l'estat `isCreating` es gestiona correctament
     * - Que el nou exemplar s'afegeix a la llista després de crear-lo
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que gestiona la creació internament
     * - Es crea un nou exemplar amb lloc="Estantería B-1" i reservat="lliure"
     * 
     * **Resultats esperats:**
     * - isCreating == false
     * - exemplars.isNotEmpty() == true
     * - error == null
     */
    @Test
    fun createExemplar_exitos_afegeixALlista() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadLlibres()
        advanceUntilIdle()

        val llibre = vm.llibresState.value.llibres.first()
        val nouExemplar = Exemplar(
            id = null,
            lloc = "Estantería B-1",
            reservat = "lliure",
            llibre = llibre
        )

        vm.createExemplar(nouExemplar)
        advanceUntilIdle()

        val state = vm.exemplarsState.value
        assertFalse("No hauria d'estar creant", state.isCreating)
        assertTrue("Hauria de tenir exemplars", state.exemplars.isNotEmpty())
    }

    /**
     * Test: Actualització d'exemplar amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot actualitzar un exemplar existent correctament
     * - Que l'estat `isUpdating` es gestiona correctament
     * - Que l'exemplar s'actualitza amb les noves dades
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que gestiona l'actualització internament
     * - Es carrega un exemplar inicial
     * - S'actualitza l'estat de l'exemplar a reservat="prestat"
     * 
     * **Resultats esperats:**
     * - isUpdating == null
     * - error == null
     * - exemplars conté l'exemplar actualitzat amb reservat="prestat"
     */
    @Test
    fun updateExemplar_exitos_actualitzaExemplar() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadExemplars()
        advanceUntilIdle()

        val exemplar = vm.exemplarsState.value.exemplars.first()
        val exemplarActualitzat = exemplar.copy(reservat = "prestat")

        vm.updateExemplar(exemplar.id!!, exemplarActualitzat)
        advanceUntilIdle()

        val state = vm.exemplarsState.value
        assertNull("No hauria d'estar actualitzant", state.isUpdating)
        assertTrue(
            "Estat hauria d'estar actualitzat",
            state.exemplars.any { it.id == exemplar.id && it.reservat == "prestat" }
        )
    }

    /**
     * Test: Eliminació d'exemplar amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot eliminar un exemplar existent correctament
     * - Que l'estat `isDeleting` es gestiona correctament
     * - Que l'exemplar s'elimina de la llista
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que gestiona l'eliminació internament
     * - Es carrega un exemplar inicial
     * - S'elimina l'exemplar de la llista
     * 
     * **Resultats esperats:**
     * - isDeleting == null
     * - exemplars no conté l'exemplar eliminat
     * - error == null
     */
    @Test
    fun deleteExemplar_exitos_eliminaExemplar() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadExemplars()
        advanceUntilIdle()

        val exemplarId = vm.exemplarsState.value.exemplars.first().id!!
        vm.deleteExemplar(exemplarId)
        advanceUntilIdle()

        val state = vm.exemplarsState.value
        assertNull("No hauria d'estar eliminant", state.isDeleting)
        assertFalse(
            "No hauria de contenir l'exemplar eliminat",
            state.exemplars.any { it.id == exemplarId }
        )
    }
}





