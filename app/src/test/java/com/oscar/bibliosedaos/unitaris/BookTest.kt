package com.oscar.bibliosedaos.unitaris

import com.oscar.bibliosedaos.data.models.Autor
import com.oscar.bibliosedaos.data.models.CreatePrestecRequest
import com.oscar.bibliosedaos.data.models.Exemplar
import com.oscar.bibliosedaos.data.models.Llibre
import com.oscar.bibliosedaos.data.network.AuthApiService
import com.oscar.bibliosedaos.data.network.AuthenticationRequest
import com.oscar.bibliosedaos.data.network.CreateUserRequest
import com.oscar.bibliosedaos.data.network.UpdateUserRequest
import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseBookViewModelTest {

    protected val dispatcher = StandardTestDispatcher()

    @Before
    open fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    open fun tearDown() {
        Dispatchers.resetMain()
    }
}

/**
 * Tests unitaris per a la gestió de llibres en BookViewModel.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la funcionalitat relacionada amb la gestió de llibres
 * en el BookViewModel utilitzant APIs falses (FakeBookApiSuccess, FakeBookApiError).
 * 
 * **Cobertura:**
 * - Càrrega de llibres (casos d'èxit i error)
 * - Creació de nous llibres
 * - Actualització de llibres existents
 * - Eliminació de llibres
 * - Gestió d'estats (isLoading, error, isCreating, isUpdating, isDeleting)
 * 
  *
 * @author Oscar
 * @since 1.0
 * @see BookViewModel
 * @see FakeBookApiSuccess
 * @see FakeBookApiError
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookTest : BaseBookViewModelTest() {

    /**
     * Test: Càrrega de llibres amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar llibres correctament des de l'API falsa
     * - Que l'estat `isLoading` es gestiona correctament (inicia a true, finalitza a false)
     * - Que la llista de llibres es carrega correctament
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que retorna 1 llibre inicial
     * 
     * **Resultats esperats:**
     * - isLoading == false
     * - llibres.isNotEmpty() == true
     * - llibres.size == 1
     * - error == null
     */
    @Test
    fun loadLlibres_exitos_retornaLlista() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadLlibres()
        advanceUntilIdle()

        val state = vm.llibresState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir llibres", state.llibres.isNotEmpty())
        assertEquals("Hauria de tenir 1 llibre", 1, state.llibres.size)
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Càrrega de llibres amb error.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament els errors quan l'API falla
     * - Que l'estat `isLoading` es gestiona correctament (finalitza a false)
     * - Que l'estat `error` conté el missatge d'error
     * - Que la llista de llibres no es carrega quan hi ha error
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiError que llança Exception("Error de xarxa")
     * 
     * **Resultats esperats:**
     * - isLoading == false
     * - error != null
     * - error.contains("Error") == true
     */
    @Test
    fun loadLlibres_error_mostraMissatgeError() = runTest {
        val vm = BookViewModel(api = FakeBookApiError())
        vm.loadLlibres()
        advanceUntilIdle()

        val state = vm.llibresState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir error", state.error != null)
        assertTrue("Error hauria de contenir 'Error'", state.error!!.contains("Error"))
    }

    /**
     * Test: Creació de llibre amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot crear un nou llibre correctament
     * - Que l'estat `isCreating` es gestiona correctament
     * - Que el nou llibre s'afegeix a la llista després de crear-lo
     * - Que no hi ha errors en la operació
     * - Que després de crear, la llista conté el nou llibre
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que gestiona la creació internament
     * - Inicialment hi ha 1 llibre
     * - Es crea un nou llibre "El amor en los tiempos del cólera"
     * 
     * **Resultats esperats:**
     * - isCreating == false
     * - llibres.size == 2 (1 inicial + 1 nou)
     * - error == null
     * - llibres conté el nou llibre amb el títol "El amor en los tiempos del cólera"
     */
    @Test
    fun createLlibre_exitos_afegeixALlista() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadLlibres()
        advanceUntilIdle()

        val initialState = vm.llibresState.value
        assertEquals("Inicialment hauria de tenir 1 llibre", 1, initialState.llibres.size)

        val nouLlibre = Llibre(
            id = null,
            isbn = "978-84-376-0495-4",
            titol = "El amor en los tiempos del cólera",
            pagines = 464,
            editorial = "Cátedra",
            autor = Autor(id = 1L, nom = "Gabriel García Márquez")
        )

        vm.createLlibre(nouLlibre)
        advanceUntilIdle()

        vm.loadLlibres()
        advanceUntilIdle()

        val state = vm.llibresState.value
        assertFalse("No hauria d'estar creant", state.isCreating)
        assertEquals("Hauria de tenir 2 llibres", 2, state.llibres.size)
        assertNull("No hauria d'haver error", state.error)
        assertTrue(
            "Hauria de contenir el nou llibre",
            state.llibres.any { it.titol == "El amor en los tiempos del cólera" }
        )
    }

    /**
     * Test: Actualització de llibre amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot actualitzar un llibre existent correctament
     * - Que l'estat `isUpdating` es gestiona correctament
     * - Que el llibre s'actualitza amb les noves dades
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que gestiona l'actualització internament
     * - Es carrega un llibre inicial
     * - S'actualitza el títol del llibre a "Cien años de soledad - Edición actualizada"
     * 
     * **Resultats esperats:**
     * - isUpdating == null
     * - error == null
     * - llibres conté el llibre actualitzat amb el nou títol
     */
    @Test
    fun updateLlibre_exitos_actualitzaLlibre() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadLlibres()
        advanceUntilIdle()

        val llibreActualitzat = vm.llibresState.value.llibres.first().copy(
            titol = "Cien años de soledad - Edición actualizada"
        )

        vm.updateLlibre(1L, llibreActualitzat)
        advanceUntilIdle()

        val state = vm.llibresState.value
        assertNull("No hauria d'estar actualitzant", state.isUpdating)
        assertTrue(
            "Títol hauria d'estar actualitzat",
            state.llibres.any { it.titol.contains("Edición actualizada") }
        )
    }

    /**
     * Test: Eliminació de llibre amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot eliminar un llibre existent correctament
     * - Que l'estat `isDeleting` es gestiona correctament
     * - Que el llibre s'elimina de la llista
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeBookApiSuccess que gestiona l'eliminació internament
     * - Inicialment hi ha 1 llibre amb id=1L
     * - S'elimina el llibre amb id=1L
     * 
     * **Resultats esperats:**
     * - isDeleting == null
     * - llibres.size == 0 (llibre eliminat)
     * - error == null
     */
    @Test
    fun deleteLlibre_exitos_eliminaLlibre() = runTest {
        val vm = BookViewModel(api = FakeBookApiSuccess())
        vm.loadLlibres()
        advanceUntilIdle()

        vm.deleteLlibre(1L)
        advanceUntilIdle()

        val state = vm.llibresState.value
        assertNull("No hauria d'estar eliminant", state.isDeleting)
        assertEquals("Hauria de tenir 0 llibres", 0, state.llibres.size)
    }
}

internal class FakeBookApiSuccess : AuthApiService {
    private val llibres = mutableListOf<Llibre>()
    private val autors = mutableListOf<Autor>()
    private val exemplars = mutableListOf<Exemplar>()

    init {
        val autor1 = Autor(id = 1L, nom = "Gabriel García Márquez")
        autors.add(autor1)

        val llibre1 = Llibre(
            id = 1L,
            isbn = "978-84-376-0494-7",
            titol = "Cien años de soledad",
            pagines = 471,
            editorial = "Cátedra",
            autor = autor1
        )
        llibres.add(llibre1)

        val exemplar1 = Exemplar(
            id = 1L,
            lloc = "Estantería A-1",
            reservat = "lliure",
            llibre = llibre1
        )
        exemplars.add(exemplar1)
    }

    override suspend fun getAllLlibres(): List<Llibre> = llibres

    override suspend fun addLlibre(llibre: Llibre): Llibre {
        val newLlibre = llibre.copy(id = (llibres.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1)
        llibres.add(newLlibre)
        return newLlibre
    }

    override suspend fun updateLlibre(id: Long, llibre: Llibre): Llibre {
        val index = llibres.indexOfFirst { it.id == id }
        return if (index >= 0) {
            val updated = llibre.copy(id = id)
            llibres[index] = updated
            updated
        } else {
            throw Exception("Llibre no trobat")
        }
    }

    override suspend fun deleteLlibre(id: Long): Response<ResponseBody> {
        llibres.removeAll { it.id == id }
        val responseBody = "Llibre esborrat".toResponseBody("text/plain".toMediaType())
        return Response.success(responseBody)
    }

    override suspend fun getLlibreById(id: Long): Llibre {
        return llibres.firstOrNull { it.id == id } ?: throw Exception("Llibre no trobat")
    }

    override suspend fun getAllAutors(): List<Autor> = autors

    override suspend fun addAutor(autor: Autor): Autor {
        val newAutor = autor.copy(id = (autors.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1)
        autors.add(newAutor)
        return newAutor
    }

    override suspend fun deleteAutor(id: Long): Response<ResponseBody> {
        autors.removeAll { it.id == id }
        val responseBody = "Autor esborrat".toResponseBody("text/plain".toMediaType())
        return Response.success(responseBody)
    }

    override suspend fun getAllExemplars(): List<Exemplar> = exemplars

    override suspend fun getExemplarsLliures(titol: String?, autor: String?): List<Exemplar> {
        return exemplars.filter { it.reservat == "lliure" }
    }

    override suspend fun addExemplar(exemplar: Exemplar): Exemplar {
        val newExemplar = exemplar.copy(id = (exemplars.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1)
        exemplars.add(newExemplar)
        return newExemplar
    }

    override suspend fun updateExemplar(id: Long, exemplar: Exemplar): Exemplar {
        val index = exemplars.indexOfFirst { it.id == id }
        return if (index >= 0) {
            val updated = exemplar.copy(id = id)
            exemplars[index] = updated
            updated
        } else {
            throw Exception("Exemplar no trobat")
        }
    }

    override suspend fun deleteExemplar(id: Long): Response<ResponseBody> {
        exemplars.removeAll { it.id == id }
        val responseBody = "Exemplar esborrat".toResponseBody("text/plain".toMediaType())
        return Response.success(responseBody)
    }

    override suspend fun getExemplarById(id: Long): Exemplar {
        return exemplars.firstOrNull { it.id == id } ?: throw Exception("Exemplar no trobat")
    }

    override suspend fun login(request: AuthenticationRequest) = error("no utilitzat")
    override suspend fun getAllUsers() = error("no utilitzat")
    override suspend fun getUserById(userId: Long) = error("no utilitzat")
    override suspend fun updateUser(userId: Long, user: UpdateUserRequest) = error("no utilitzat")
    override suspend fun deleteUser(userId: Long) = error("no utilitzat")
    override suspend fun createUser(request: CreateUserRequest) = error("no utilitzat")
    override suspend fun logout() = error("no utilitzat")
    override suspend fun getUserByNif(nif: String) = error("no utilitzat")
    override suspend fun getPrestecsActius(usuariId: Long?) = error("no utilitzat")
    override suspend fun getAllPrestecs(usuariId: Long?) = error("no utilitzat")
    override suspend fun createPrestec(prestec: CreatePrestecRequest) = error("no utilitzat")
    override suspend fun retornarPrestec(prestecId: Long?) = error("no utilitzat")
    override suspend fun getAllHoraris(): List<com.oscar.bibliosedaos.data.models.Horari> = error("no utilitzat")
    override suspend fun createHorari(horari: com.oscar.bibliosedaos.data.models.Horari): com.oscar.bibliosedaos.data.models.Horari = error("no utilitzat")
    override suspend fun getAllGrups(): List<com.oscar.bibliosedaos.data.models.Grup> = error("no utilitzat")
    override suspend fun createGrup(grup: com.oscar.bibliosedaos.data.models.Grup): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
    override suspend fun deleteGrup(id: Long): Response<ResponseBody> = error("no utilitzat")
    override suspend fun addMemberToGrup(grupId: Long, membreId: Long): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
    override suspend fun getMembresGrup(grupId: Long): List<com.oscar.bibliosedaos.data.network.User> = error("no utilitzat")
    override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
}

internal class FakeBookApiError : AuthApiService {
    override suspend fun getAllLlibres(): List<Llibre> {
        throw Exception("Error de xarxa")
    }

    override suspend fun addLlibre(llibre: Llibre): Llibre {
        throw Exception("Error creant llibre")
    }

    override suspend fun updateLlibre(id: Long, llibre: Llibre): Llibre {
        throw Exception("Error actualitzant llibre")
    }

    override suspend fun deleteLlibre(id: Long): Response<ResponseBody> {
        throw Exception("Error eliminant llibre")
    }

    override suspend fun getLlibreById(id: Long): Llibre {
        throw Exception("Llibre no trobat")
    }

    override suspend fun getAllAutors(): List<Autor> {
        throw Exception("Error de xarxa")
    }

    override suspend fun addAutor(autor: Autor): Autor {
        throw Exception("Error creant autor")
    }

    override suspend fun deleteAutor(id: Long): Response<ResponseBody> {
        throw Exception("Error eliminant autor")
    }

    override suspend fun getAllExemplars(): List<Exemplar> {
        throw Exception("Error de xarxa")
    }

    override suspend fun getExemplarsLliures(titol: String?, autor: String?): List<Exemplar> {
        throw Exception("Error cercant exemplars")
    }

    override suspend fun addExemplar(exemplar: Exemplar): Exemplar {
        throw Exception("Error creant exemplar")
    }

    override suspend fun updateExemplar(id: Long, exemplar: Exemplar): Exemplar {
        throw Exception("Error actualitzant exemplar")
    }

    override suspend fun deleteExemplar(id: Long): Response<ResponseBody> {
        throw Exception("Error eliminant exemplar")
    }

    override suspend fun getExemplarById(id: Long): Exemplar {
        throw Exception("Exemplar no trobat")
    }

    override suspend fun login(request: AuthenticationRequest) = error("no utilitzat")
    override suspend fun getAllUsers() = error("no utilitzat")
    override suspend fun getUserById(userId: Long) = error("no utilitzat")
    override suspend fun updateUser(userId: Long, user: UpdateUserRequest) = error("no utilitzat")
    override suspend fun deleteUser(userId: Long) = error("no utilitzat")
    override suspend fun createUser(request: CreateUserRequest) = error("no utilitzat")
    override suspend fun logout() = error("no utilitzat")
    override suspend fun getUserByNif(nif: String) = error("no utilitzat")
    override suspend fun getPrestecsActius(usuariId: Long?) = error("no utilitzat")
    override suspend fun getAllPrestecs(usuariId: Long?) = error("no utilitzat")
    override suspend fun createPrestec(prestec: CreatePrestecRequest) = error("no utilitzat")
    override suspend fun retornarPrestec(prestecId: Long?) = error("no utilitzat")
    override suspend fun getAllHoraris(): List<com.oscar.bibliosedaos.data.models.Horari> = error("no utilitzat")
    override suspend fun createHorari(horari: com.oscar.bibliosedaos.data.models.Horari): com.oscar.bibliosedaos.data.models.Horari = error("no utilitzat")
    override suspend fun getAllGrups(): List<com.oscar.bibliosedaos.data.models.Grup> = error("no utilitzat")
    override suspend fun createGrup(grup: com.oscar.bibliosedaos.data.models.Grup): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
    override suspend fun deleteGrup(id: Long): Response<ResponseBody> = error("no utilitzat")
    override suspend fun addMemberToGrup(grupId: Long, membreId: Long): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
    override suspend fun getMembresGrup(grupId: Long): List<com.oscar.bibliosedaos.data.network.User> = error("no utilitzat")
    override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
}


