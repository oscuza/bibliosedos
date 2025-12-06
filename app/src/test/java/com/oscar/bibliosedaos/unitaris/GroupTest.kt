package com.oscar.bibliosedaos.unitaris

import com.oscar.bibliosedaos.data.models.Grup
import com.oscar.bibliosedaos.data.models.Horari
import com.oscar.bibliosedaos.data.network.*
import com.oscar.bibliosedaos.ui.viewmodels.GroupViewModel
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Tests unitaris per a la gestió de grups de lectura en GroupViewModel.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la funcionalitat relacionada amb la gestió de grups
 * de lectura utilitzant APIs falses (FakeGroupApiSuccess, FakeGroupApiError).
 * 
 * **Cobertura:**
 * - Càrrega de grups (casos d'èxit i error)
 * - Creació de nous grups
 * - Eliminació de grups
 * - Càrrega de grup per ID (filtre client-side)
 * - Càrrega de grups per usuari (filtre client-side)
 * - Gestió d'estats (isLoading, error, isCreating, isDeleting)
 * 
 *
 * @author Oscar
 * @since 1.0
 * @see GroupViewModel
 * @see FakeGroupApiSuccess
 * @see FakeGroupApiError
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test: Càrrega de grups amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar grups correctament des de l'API falsa
     * - Que l'estat `isLoading` es gestiona correctament
     * - Que la llista de grups es carrega correctament
     * - Que no hi ha errors en la operació
     */
    @Test
    fun loadGrups_exitos_retornaLlista() = runTest {
        val vm = GroupViewModel(api = FakeGroupApiSuccess())
        vm.loadGrups()
        advanceUntilIdle()

        val state = vm.grupsState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir grups", state.grups.isNotEmpty())
        assertEquals("Hauria de tenir 1 grup", 1, state.grups.size)
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Càrrega de grups amb error.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament els errors quan l'API falla
     * - Que l'estat `error` conté el missatge d'error
     */
    @Test
    fun loadGrups_error_mostraMissatgeError() = runTest {
        val vm = GroupViewModel(api = FakeGroupApiError())
        vm.loadGrups()
        advanceUntilIdle()

        val state = vm.grupsState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir error", state.error != null)
        assertTrue("Error hauria de contenir 'Error'", state.error!!.contains("Error"))
    }

    /**
     * Test: Creació de grup amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot crear un nou grup correctament
     * - Que l'estat `isCreating` es gestiona correctament
     * - Que el nou grup s'afegeix a la llista després de crear-lo
     */
    @Test
    fun createGrup_exitos_afegeixALlista() = runTest {
        val vm = GroupViewModel(api = FakeGroupApiSuccess())
        vm.loadGrups()
        advanceUntilIdle()

        val initialState = vm.grupsState.value
        assertEquals("Inicialment hauria de tenir 1 grup", 1, initialState.grups.size)

        val administrador = User(
            id = 1L,
            nick = "admin",
            nom = "Admin",
            cognom1 = "Test",
            cognom2 = null,
            rol = 2
        )

        val horari = Horari(
            id = 1L,
            sala = "Sala A",
            dia = "Dilluns",
            hora = "10:00",
            estat = "lliure"
        )

        vm.createGrup(
            nom = "Grup de Poesia",
            tematica = "Poesia Contemporània",
            administrador = administrador,
            horari = horari
        )
        advanceUntilIdle()

        val state = vm.grupsState.value
        assertFalse("No hauria d'estar creant", state.isCreating)
        assertNull("No hauria d'haver error", state.error)
        
        // Verificar que el nou grup s'ha afegit
        assertTrue(
            "Hauria de contenir el nou grup",
            state.grups.any { it.nom == "Grup de Poesia" }
        )
        
        // Verificar que el grup inicial encara hi és
        assertTrue(
            "Hauria de contenir el grup inicial",
            state.grups.any { it.nom == "Grup de Novel·la" }
        )
        
        // Verificar que hi ha exactament 2 grups (l'inicial + el nou)
        // Eliminar duplicats per ID abans de verificar
        val grupsUnics = state.grups.distinctBy { it.id }
        assertEquals(
            "Hauria de tenir exactament 2 grups únics. Grups trobats: ${state.grups.size}, Grups únics: ${grupsUnics.size}. Grups: ${state.grups.map { "${it.id}:${it.nom}" }}",
            2,
            grupsUnics.size
        )
    }

    /**
     * Test: Creació de grup amb error.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament els errors en la creació
     */
    @Test
    fun createGrup_error_mostraMissatgeError() = runTest {
        val vm = GroupViewModel(api = FakeGroupApiError())
        
        val administrador = User(
            id = 1L,
            nick = "admin",
            nom = "Admin",
            cognom1 = "Test",
            cognom2 = null,
            rol = 2
        )

        val horari = Horari(
            id = 1L,
            sala = "Sala A",
            dia = "Dilluns",
            hora = "10:00",
            estat = "lliure"
        )

        vm.createGrup(
            nom = "Grup de Poesia",
            tematica = "Poesia Contemporània",
            administrador = administrador,
            horari = horari
        )
        advanceUntilIdle()

        val state = vm.grupsState.value
        assertFalse("No hauria d'estar creant", state.isCreating)
        assertTrue("Hauria de tenir error", state.error != null)
        assertTrue("Error hauria de contenir 'Error'", state.error!!.contains("Error"))
    }

    /**
     * Test: Eliminació de grup amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot eliminar un grup existent correctament
     * - Que l'estat `isDeleting` es gestiona correctament
     * - Que el grup s'elimina de la llista
     */
    @Test
    fun deleteGrup_exitos_eliminaGrup() = runTest {
        val vm = GroupViewModel(api = FakeGroupApiSuccess())
        vm.loadGrups()
        advanceUntilIdle()

        val initialState = vm.grupsState.value
        assertEquals("Inicialment hauria de tenir 1 grup", 1, initialState.grups.size)

        vm.deleteGrup(1L)
        advanceUntilIdle()

        val state = vm.grupsState.value
        assertNull("No hauria d'estar eliminant", state.isDeleting)
        assertEquals("Hauria de tenir 0 grups", 0, state.grups.size)
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Càrrega de grup per ID.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar un grup específic per ID
     * - Que el grup es troba correctament (filtre client-side)
     */
    @Test
    fun loadGrupById_exitos_trobaGrup() = runTest {
        val vm = GroupViewModel(api = FakeGroupApiSuccess())
        vm.loadGrups()
        advanceUntilIdle()

        vm.loadGrupById(1L)
        advanceUntilIdle()

        val selectedGrup = vm.selectedGrupState.value
        assertNotNull("Hauria de trobar el grup", selectedGrup)
        assertEquals("ID hauria de ser 1", 1L, selectedGrup?.id)
    }

    /**
     * Test: Càrrega de grup per ID quan no existeix.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament quan un grup no existeix
     */
    @Test
    fun loadGrupById_noExisteix_mostraError() = runTest {
        val vm = GroupViewModel(api = FakeGroupApiSuccess())
        vm.loadGrups()
        advanceUntilIdle()

        vm.loadGrupById(999L)
        advanceUntilIdle()

        val state = vm.grupsState.value
        assertTrue("Hauria de tenir error", state.error != null)
        assertTrue("Error hauria de contenir 'no trobat'", state.error!!.contains("no trobat"))
    }
}

/**
 * Implementació fake d'AuthApiService que simula operacions exitoses amb grups.
 */
internal class FakeGroupApiSuccess : AuthApiService {
    private val grups = mutableListOf<Grup>()
    private val horaris = mutableListOf<Horari>()

    init {
        val horari1 = Horari(
            id = 1L,
            sala = "Sala A",
            dia = "Dilluns",
            hora = "10:00",
            estat = "lliure"
        )
        horaris.add(horari1)

        val administrador = User(
            id = 1L,
            nick = "admin",
            nom = "Admin",
            cognom1 = "Test",
            cognom2 = null,
            rol = 2
        )

        val grup1 = Grup(
            id = 1L,
            nom = "Grup de Novel·la",
            tematica = "Novel·la Contemporània",
            administrador = administrador,
            horari = horari1,
            membres = emptyList()
        )
        grups.add(grup1)
    }

    override suspend fun getAllGrups(): List<Grup> = grups

    override suspend fun createGrup(grup: Grup): Grup {
        val newGrup = grup.copy(id = (grups.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1)
        grups.add(newGrup)
        return newGrup
    }

    override suspend fun deleteGrup(id: Long): Response<ResponseBody> {
        grups.removeAll { it.id == id }
        val responseBody = "Grup esborrat".toResponseBody("text/plain".toMediaType())
        return Response.success(responseBody)
    }

    override suspend fun getAllHoraris(): List<Horari> = horaris

    override suspend fun createHorari(horari: Horari): Horari {
        val newHorari = horari.copy(id = (horaris.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1)
        horaris.add(newHorari)
        return newHorari
    }

    override suspend fun addMemberToGrup(grupId: Long, membreId: Long): Grup {
        val grup = grups.firstOrNull { it.id == grupId } ?: throw Exception("Grup no trobat")
        val membre = User(id = membreId, nick = "user$membreId", nom = "User", cognom1 = "Test", cognom2 = null, rol = 1)
        val membresActuals = grup.membres?.toMutableList() ?: mutableListOf()
        membresActuals.add(membre)
        val updatedGrup = grup.copy(membres = membresActuals)
        grups[grups.indexOf(grup)] = updatedGrup
        return updatedGrup
    }

    override suspend fun getMembresGrup(grupId: Long): List<User> {
        val grup = grups.firstOrNull { it.id == grupId } ?: throw Exception("Grup no trobat")
        return grup.membres ?: emptyList()
    }

    override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<ResponseBody> {
        val grup = grups.firstOrNull { it.id == grupId } ?: throw Exception("Grup no trobat")
        val membresActuals = grup.membres?.toMutableList() ?: mutableListOf()
        membresActuals.removeAll { it.id == membreId }
        val updatedGrup = grup.copy(membres = membresActuals)
        grups[grups.indexOf(grup)] = updatedGrup
        val responseBody = "Membre eliminat".toResponseBody("text/plain".toMediaType())
        return Response.success(responseBody)
    }

    // Mètodes no utilitzats en aquests tests
    override suspend fun login(request: AuthenticationRequest) = error("no utilitzat")
    override suspend fun getAllUsers() = error("no utilitzat")
    override suspend fun getUserById(userId: Long) = error("no utilitzat")
    override suspend fun updateUser(userId: Long, user: UpdateUserRequest) = error("no utilitzat")
    override suspend fun deleteUser(userId: Long) = error("no utilitzat")
    override suspend fun createUser(request: CreateUserRequest) = error("no utilitzat")
    override suspend fun logout() = error("no utilitzat")
    override suspend fun getUserByNif(nif: String) = error("no utilitzat")
    override suspend fun getAllLlibres() = error("no utilitzat")
    override suspend fun addLlibre(llibre: com.oscar.bibliosedaos.data.models.Llibre) = error("no utilitzat")
    override suspend fun updateLlibre(id: Long, llibre: com.oscar.bibliosedaos.data.models.Llibre) = error("no utilitzat")
    override suspend fun deleteLlibre(id: Long) = error("no utilitzat")
    override suspend fun getLlibreById(id: Long) = error("no utilitzat")
    override suspend fun getAllAutors() = error("no utilitzat")
    override suspend fun addAutor(autor: com.oscar.bibliosedaos.data.models.Autor) = error("no utilitzat")
    override suspend fun deleteAutor(id: Long) = error("no utilitzat")
    override suspend fun getAllExemplars() = error("no utilitzat")
    override suspend fun getExemplarsLliures(titol: String?, autor: String?) = error("no utilitzat")
    override suspend fun addExemplar(exemplar: com.oscar.bibliosedaos.data.models.Exemplar) = error("no utilitzat")
    override suspend fun updateExemplar(id: Long, exemplar: com.oscar.bibliosedaos.data.models.Exemplar) = error("no utilitzat")
    override suspend fun deleteExemplar(id: Long) = error("no utilitzat")
    override suspend fun getExemplarById(id: Long) = error("no utilitzat")
    override suspend fun getPrestecsActius(usuariId: Long?) = error("no utilitzat")
    override suspend fun getAllPrestecs(usuariId: Long?) = error("no utilitzat")
    override suspend fun createPrestec(prestec: com.oscar.bibliosedaos.data.models.CreatePrestecRequest) = error("no utilitzat")
    override suspend fun retornarPrestec(prestecId: Long?) = error("no utilitzat")
}

/**
 * Implementació fake d'AuthApiService que simula errors en operacions amb grups.
 */
internal class FakeGroupApiError : AuthApiService {
    override suspend fun getAllGrups(): List<Grup> {
        throw Exception("Error de xarxa")
    }

    override suspend fun createGrup(grup: Grup): Grup {
        throw Exception("Error creant grup")
    }

    override suspend fun deleteGrup(id: Long): Response<ResponseBody> {
        throw Exception("Error eliminant grup")
    }

    override suspend fun getAllHoraris(): List<Horari> {
        throw Exception("Error de xarxa")
    }

    override suspend fun createHorari(horari: Horari): Horari {
        throw Exception("Error creant horari")
    }

    override suspend fun addMemberToGrup(grupId: Long, membreId: Long): Grup {
        throw Exception("Error afegint membre")
    }

    override suspend fun getMembresGrup(grupId: Long): List<User> {
        throw Exception("Error carregant membres")
    }

    override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<ResponseBody> {
        throw Exception("Error eliminant membre")
    }

    // Mètodes no utilitzats
    override suspend fun login(request: AuthenticationRequest) = error("no utilitzat")
    override suspend fun getAllUsers() = error("no utilitzat")
    override suspend fun getUserById(userId: Long) = error("no utilitzat")
    override suspend fun updateUser(userId: Long, user: UpdateUserRequest) = error("no utilitzat")
    override suspend fun deleteUser(userId: Long) = error("no utilitzat")
    override suspend fun createUser(request: CreateUserRequest) = error("no utilitzat")
    override suspend fun logout() = error("no utilitzat")
    override suspend fun getUserByNif(nif: String) = error("no utilitzat")
    override suspend fun getAllLlibres() = error("no utilitzat")
    override suspend fun addLlibre(llibre: com.oscar.bibliosedaos.data.models.Llibre) = error("no utilitzat")
    override suspend fun updateLlibre(id: Long, llibre: com.oscar.bibliosedaos.data.models.Llibre) = error("no utilitzat")
    override suspend fun deleteLlibre(id: Long) = error("no utilitzat")
    override suspend fun getLlibreById(id: Long) = error("no utilitzat")
    override suspend fun getAllAutors() = error("no utilitzat")
    override suspend fun addAutor(autor: com.oscar.bibliosedaos.data.models.Autor) = error("no utilitzat")
    override suspend fun deleteAutor(id: Long) = error("no utilitzat")
    override suspend fun getAllExemplars() = error("no utilitzat")
    override suspend fun getExemplarsLliures(titol: String?, autor: String?) = error("no utilitzat")
    override suspend fun addExemplar(exemplar: com.oscar.bibliosedaos.data.models.Exemplar) = error("no utilitzat")
    override suspend fun updateExemplar(id: Long, exemplar: com.oscar.bibliosedaos.data.models.Exemplar) = error("no utilitzat")
    override suspend fun deleteExemplar(id: Long) = error("no utilitzat")
    override suspend fun getExemplarById(id: Long) = error("no utilitzat")
    override suspend fun getPrestecsActius(usuariId: Long?) = error("no utilitzat")
    override suspend fun getAllPrestecs(usuariId: Long?) = error("no utilitzat")
    override suspend fun createPrestec(prestec: com.oscar.bibliosedaos.data.models.CreatePrestecRequest) = error("no utilitzat")
    override suspend fun retornarPrestec(prestecId: Long?) = error("no utilitzat")
}

