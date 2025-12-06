package com.oscar.bibliosedaos.unitaris

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
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Tests unitaris per a la gestió d'horaris en GroupViewModel.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la funcionalitat relacionada amb la gestió d'horaris
 * utilitzant APIs falses (FakeHorariApiSuccess, FakeHorariApiError).
 * 
 * **Cobertura:**
 * - Càrrega d'horaris (casos d'èxit i error)
 * - Creació de nous horaris
 * - Filtrat d'horaris lliures (client-side)
 * - Gestió d'estats (isLoading, error, isCreating)
 * 
 *
 * @author Oscar
 * @since 1.0
 * @see GroupViewModel
 * @see FakeHorariApiSuccess
 * @see FakeHorariApiError
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HorariTest {

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
     * Test: Càrrega d'horaris amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar horaris correctament des de l'API falsa
     * - Que l'estat `isLoading` es gestiona correctament
     * - Que la llista d'horaris es carrega correctament
     * - Que no hi ha errors en la operació
     */
    @Test
    fun loadHoraris_exitos_retornaLlista() = runTest {
        val vm = GroupViewModel(api = FakeHorariApiSuccess())
        vm.loadHoraris()
        advanceUntilIdle()

        val state = vm.horarisState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir horaris", state.horaris.isNotEmpty())
        assertEquals("Hauria de tenir 2 horaris", 2, state.horaris.size)
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Càrrega d'horaris amb error.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament els errors quan l'API falla
     * - Que l'estat `error` conté el missatge d'error
     */
    @Test
    fun loadHoraris_error_mostraMissatgeError() = runTest {
        val vm = GroupViewModel(api = FakeHorariApiError())
        vm.loadHoraris()
        advanceUntilIdle()

        val state = vm.horarisState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir error", state.error != null)
        assertTrue("Error hauria de contenir 'Error'", state.error!!.contains("Error"))
    }

    /**
     * Test: Creació d'horari amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot crear un nou horari correctament
     * - Que l'estat `isCreating` es gestiona correctament
     * - Que el nou horari s'afegeix a la llista després de crear-lo
     */
    @Test
    fun createHorari_exitos_afegeixALlista() = runTest {
        val vm = GroupViewModel(api = FakeHorariApiSuccess())
        vm.loadHoraris()
        advanceUntilIdle()

        val initialState = vm.horarisState.value
        assertEquals("Inicialment hauria de tenir 2 horaris", 2, initialState.horaris.size)

        vm.createHorari(
            sala = "Sala C",
            dia = "Dimecres",
            hora = "15:00",
            estat = "lliure"
        )
        advanceUntilIdle()

        val state = vm.horarisState.value
        assertFalse("No hauria d'estar creant", state.isCreating)
        assertNull("No hauria d'haver error", state.error)
        
        // Verificar que el nou horari s'ha afegit
        assertTrue(
            "Hauria de contenir el nou horari",
            state.horaris.any { it.sala == "Sala C" && it.dia == "Dimecres" }
        )
        
        // Verificar que hi ha exactament 3 horaris (2 inicials + el nou)
        // Eliminar duplicats per ID abans de verificar
        val horarisUnics = state.horaris.distinctBy { it.id }
        assertEquals(
            "Hauria de tenir exactament 3 horaris únics. Horaris trobats: ${state.horaris.size}, Horaris únics: ${horarisUnics.size}. Horaris: ${state.horaris.map { "${it.id}:${it.sala}-${it.dia}-${it.hora}" }}",
            3,
            horarisUnics.size
        )
    }

    /**
     * Test: Creació d'horari amb error.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament els errors en la creació
     */
    @Test
    fun createHorari_error_mostraMissatgeError() = runTest {
        val vm = GroupViewModel(api = FakeHorariApiError())
        
        vm.createHorari(
            sala = "Sala C",
            dia = "Dimecres",
            hora = "15:00",
            estat = "lliure"
        )
        advanceUntilIdle()

        val state = vm.horarisState.value
        assertFalse("No hauria d'estar creant", state.isCreating)
        assertTrue("Hauria de tenir error", state.error != null)
        assertTrue("Error hauria de contenir 'Error'", state.error!!.contains("Error"))
    }

    /**
     * Test: Filtrat d'horaris lliures.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot filtrar correctament els horaris lliures
     * - Que la funció `getHorarisLliures()` retorna només horaris amb estat "lliure"
     */
    @Test
    fun getHorarisLliures_exitos_filtraCorrectament() = runTest {
        val vm = GroupViewModel(api = FakeHorariApiSuccess())
        vm.loadHoraris()
        advanceUntilIdle()

        val horarisLliures = vm.getHorarisLliures()
        assertEquals("Hauria de tenir 1 horari lliure", 1, horarisLliures.size)
        assertTrue("Tots els horaris haurien de ser lliures", horarisLliures.all { it.isLliure })
    }

    /**
     * Test: Propietat isLliure d'Horari.
     * 
     * **Què s'està provant:**
     * - Que la propietat `isLliure` funciona correctament
     * - Que retorna `true` per horaris amb estat "lliure" o null
     * - Que retorna `false` per horaris amb estat "reservat"
     */
    @Test
    fun horari_isLliure_funcionaCorrectament() = runTest {
        val horariLliure = Horari(
            id = 1L,
            sala = "Sala A",
            dia = "Dilluns",
            hora = "10:00",
            estat = "lliure"
        )
        assertTrue("Horari amb estat 'lliure' hauria de ser lliure", horariLliure.isLliure)

        val horariReservat = Horari(
            id = 2L,
            sala = "Sala B",
            dia = "Dimarts",
            hora = "14:00",
            estat = "reservat"
        )
        assertFalse("Horari amb estat 'reservat' no hauria de ser lliure", horariReservat.isLliure)

        val horariNull = Horari(
            id = 3L,
            sala = "Sala C",
            dia = "Dimecres",
            hora = "16:00",
            estat = null
        )
        assertTrue("Horari amb estat null hauria de ser lliure", horariNull.isLliure)
    }
}

/**
 * Implementació fake d'AuthApiService que simula operacions exitoses amb horaris.
 */
internal class FakeHorariApiSuccess : AuthApiService {
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

        val horari2 = Horari(
            id = 2L,
            sala = "Sala B",
            dia = "Dimarts",
            hora = "14:00",
            estat = "reservat"
        )
        horaris.add(horari2)
    }

    override suspend fun getAllHoraris(): List<Horari> = horaris

    override suspend fun createHorari(horari: Horari): Horari {
        val newHorari = horari.copy(id = (horaris.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1)
        horaris.add(newHorari)
        return newHorari
    }

    override suspend fun getAllGrups(): List<com.oscar.bibliosedaos.data.models.Grup> = emptyList()

    override suspend fun createGrup(grup: com.oscar.bibliosedaos.data.models.Grup): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
    override suspend fun deleteGrup(id: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
    override suspend fun addMemberToGrup(grupId: Long, membreId: Long): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
    override suspend fun getMembresGrup(grupId: Long): List<User> = error("no utilitzat")
    override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")

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

/**
 * Implementació fake d'AuthApiService que simula errors en operacions amb horaris.
 */
internal class FakeHorariApiError : AuthApiService {
    override suspend fun getAllHoraris(): List<Horari> {
        throw Exception("Error de xarxa")
    }

    override suspend fun createHorari(horari: Horari): Horari {
        throw Exception("Error creant horari")
    }

    override suspend fun getAllGrups(): List<com.oscar.bibliosedaos.data.models.Grup> = emptyList()
    override suspend fun createGrup(grup: com.oscar.bibliosedaos.data.models.Grup): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
    override suspend fun deleteGrup(id: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
    override suspend fun addMemberToGrup(grupId: Long, membreId: Long): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
    override suspend fun getMembresGrup(grupId: Long): List<User> = error("no utilitzat")
    override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")

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

