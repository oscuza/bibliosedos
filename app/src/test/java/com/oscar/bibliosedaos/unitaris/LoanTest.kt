package com.oscar.bibliosedaos.unitaris

import android.os.Build
import androidx.annotation.RequiresApi
import com.oscar.bibliosedaos.data.models.*
import com.oscar.bibliosedaos.data.network.AuthApiService
import com.oscar.bibliosedaos.data.network.User
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tests unitaris per a la classe LoanViewModel.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la funcionalitat relacionada amb la gestió de préstecs
 * en el LoanViewModel utilitzant APIs falses (FakeApiSuccess, FakeApiError).
 * 
 * **Cobertura:**
 * - ✅ Càrrega de préstecs actius (casos d'èxit i error)
 * - ✅ Càrrega de préstecs actius per usuari específic
 * - ✅ Càrrega d'historial de préstecs
 * - ✅ Creació de préstecs (casos d'èxit i error)
 * - ✅ Devolució de préstecs (casos d'èxit i error)
 * - ✅ Neteja d'errors
 * - ✅ Reinici de formularis
 * - ✅ Gestió d'estats (isLoading, error, isSubmitting, isReturning, success, etc.)
 * 
 *
 * @author Oscar
 * @since 1.0
 * @see LoanViewModel
 * @see FakeApiSuccess
 * @see FakeApiError
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoanTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== FAKE API IMPLEMENTATIONS ==========

    /**
     * Fake API que simula operacions exitoses amb préstecs.
     */
    private class FakeApiSuccess : AuthApiService {
        private val prestecs = mutableListOf<Prestec>()
        private var nextId = 1L

        init {
            // Dades inicials
            val usuari = User(
                id = 1L,
                nick = "testuser",
                nom = "Test",
                cognom1 = "User",
                cognom2 = null,
                rol = 1,
                nif = "12345678A"
            )

            val autor = Autor(id = 1L, nom = "Gabriel García Márquez")
            val llibre = Llibre(
                id = 1L,
                isbn = "978-84-376-0494-7",
                titol = "Cien años de soledad",
                pagines = 471,
                editorial = "Cátedra",
                autor = autor
            )

            val exemplar = Exemplar(
                id = 1L,
                lloc = "Estantería A-1",
                reservat = "prestat",
                llibre = llibre
            )

            val prestec = Prestec(
                id = 1L,
                dataPrestec = LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                dataDevolucio = null,
                usuari = usuari,
                exemplar = exemplar
            )
            prestecs.add(prestec)
        }

        override suspend fun getPrestecsActius(usuariId: Long?): List<Prestec> {
            return if (usuariId != null) {
                prestecs.filter { it.usuari?.id == usuariId && it.dataDevolucio == null }
            } else {
                prestecs.filter { it.dataDevolucio == null }
            }
        }

        override suspend fun getAllPrestecs(usuariId: Long?): List<Prestec> {
            return if (usuariId != null) {
                prestecs.filter { it.usuari?.id == usuariId }
            } else {
                prestecs
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override suspend fun createPrestec(prestec: CreatePrestecRequest): Prestec {
            val nouPrestec = Prestec(
                id = nextId++,
                dataPrestec = prestec.dataPrestec,
                dataDevolucio = null,
                usuari = User(
                    id = prestec.usuari.id,
                    nick = "user${prestec.usuari.id}",
                    nom = "User",
                    cognom1 = "Test",
                    cognom2 = null,
                    rol = 1
                ),
                exemplar = Exemplar(
                    id = prestec.exemplar.id,
                    lloc = "Estantería",
                    reservat = "prestat",
                    llibre = Llibre(
                        id = 1L,
                        isbn = "123",
                        titol = "Test Book",
                        pagines = 100,
                        editorial = "Test",
                        autor = null
                    )
                )
            )
            prestecs.add(nouPrestec)
            return nouPrestec
        }

        override suspend fun retornarPrestec(prestecId: Long?): Response<okhttp3.ResponseBody> {
            val prestec = prestecs.firstOrNull { it.id == prestecId }
            return if (prestec != null) {
                val index = prestecs.indexOf(prestec)
                val actualitzat = prestec.copy(
                    dataDevolucio = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
                prestecs[index] = actualitzat
                Response.success("Préstec retornat".toResponseBody())
            } else {
                Response.error(404, "Préstec no trobat".toResponseBody())
            }
        }

        override suspend fun getAllHoraris(): List<Horari> {
            TODO("Not yet implemented")
        }

        override suspend fun getAllGrups(): List<Grup> {
            TODO("Not yet implemented")
        }

        override suspend fun getGrupById(id: Long): Grup {
            TODO("Not yet implemented")
        }

        override suspend fun createGrup(request: CreateGrupRequest): Grup {
            TODO("Not yet implemented")
        }

        override suspend fun updateGrup(
            id: Long,
            request: UpdateGrupRequest,
        ): Grup {
            TODO("Not yet implemented")
        }

        override suspend fun deleteGrup(id: Long): Response<ResponseBody> {
            TODO("Not yet implemented")
        }

        override suspend fun addMemberToGrup(request: AddMemberRequest): Grup {
            TODO("Not yet implemented")
        }

        override suspend fun removeMemberFromGrup(
            grupId: Long,
            usuariId: Long,
        ): Response<ResponseBody> {
            TODO("Not yet implemented")
        }

        override suspend fun getGrupsByUsuari(usuariId: Long): List<Grup> {
            TODO("Not yet implemented")
        }

        // Mètodes no utilitzats
        override suspend fun login(request: com.oscar.bibliosedaos.data.network.AuthenticationRequest) =
            error("no utilitzat")
        override suspend fun getAllUsers() = error("no utilitzat")
        override suspend fun getUserById(userId: Long) = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: com.oscar.bibliosedaos.data.network.UpdateUserRequest) =
            error("no utilitzat")
        override suspend fun deleteUser(userId: Long) = error("no utilitzat")
        override suspend fun createUser(request: com.oscar.bibliosedaos.data.network.CreateUserRequest) =
            error("no utilitzat")
        override suspend fun logout() = error("no utilitzat")
        override suspend fun getUserByNif(nif: String) = error("no utilitzat")
        override suspend fun getAllLlibres() = error("no utilitzat")
        override suspend fun addLlibre(llibre: Llibre) = error("no utilitzat")
        override suspend fun updateLlibre(id: Long, llibre: Llibre) = error("no utilitzat")
        override suspend fun deleteLlibre(id: Long) = error("no utilitzat")
        override suspend fun getLlibreById(id: Long) = error("no utilitzat")
        override suspend fun getAllAutors() = error("no utilitzat")
        override suspend fun addAutor(autor: Autor) = error("no utilitzat")
        override suspend fun deleteAutor(id: Long) = error("no utilitzat")
        override suspend fun getAllExemplars() = error("no utilitzat")
        override suspend fun getExemplarsLliures(titol: String?, autor: String?) = error("no utilitzat")
        override suspend fun addExemplar(exemplar: Exemplar) = error("no utilitzat")
        override suspend fun updateExemplar(id: Long, exemplar: Exemplar) = error("no utilitzat")
        override suspend fun deleteExemplar(id: Long) = error("no utilitzat")
        override suspend fun getExemplarById(id: Long) = error("no utilitzat")
    }

    /**
     * Fake API que simula errors en les operacions.
     */
    private class FakeApiError : AuthApiService {
        override suspend fun getPrestecsActius(usuariId: Long?): List<Prestec> {
            throw Exception("Error de xarxa")
        }
        override suspend fun getAllPrestecs(usuariId: Long?): List<Prestec> {
            throw Exception("Error de xarxa")
        }
        override suspend fun createPrestec(prestec: CreatePrestecRequest): Prestec {
            throw Exception("Error creant préstec")
        }
        override suspend fun retornarPrestec(prestecId: Long?): Response<okhttp3.ResponseBody> {
            return Response.error(404, "Préstec no trobat".toResponseBody())
        }

        override suspend fun getAllHoraris(): List<Horari> {
            TODO("Not yet implemented")
        }

        override suspend fun getAllGrups(): List<Grup> {
            TODO("Not yet implemented")
        }

        override suspend fun getGrupById(id: Long): Grup {
            TODO("Not yet implemented")
        }

        override suspend fun createGrup(request: CreateGrupRequest): Grup {
            TODO("Not yet implemented")
        }

        override suspend fun updateGrup(
            id: Long,
            request: UpdateGrupRequest,
        ): Grup {
            TODO("Not yet implemented")
        }

        override suspend fun deleteGrup(id: Long): Response<ResponseBody> {
            TODO("Not yet implemented")
        }

        override suspend fun addMemberToGrup(request: AddMemberRequest): Grup {
            TODO("Not yet implemented")
        }

        override suspend fun removeMemberFromGrup(
            grupId: Long,
            usuariId: Long,
        ): Response<ResponseBody> {
            TODO("Not yet implemented")
        }

        override suspend fun getGrupsByUsuari(usuariId: Long): List<Grup> {
            TODO("Not yet implemented")
        }

        // Mètodes no utilitzats
        override suspend fun login(request: com.oscar.bibliosedaos.data.network.AuthenticationRequest) =
            error("no utilitzat")
        override suspend fun getAllUsers() = error("no utilitzat")
        override suspend fun getUserById(userId: Long) = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: com.oscar.bibliosedaos.data.network.UpdateUserRequest) =
            error("no utilitzat")
        override suspend fun deleteUser(userId: Long) = error("no utilitzat")
        override suspend fun createUser(request: com.oscar.bibliosedaos.data.network.CreateUserRequest) =
            error("no utilitzat")
        override suspend fun logout() = error("no utilitzat")
        override suspend fun getUserByNif(nif: String) = error("no utilitzat")
        override suspend fun getAllLlibres() = error("no utilitzat")
        override suspend fun addLlibre(llibre: Llibre) = error("no utilitzat")
        override suspend fun updateLlibre(id: Long, llibre: Llibre) = error("no utilitzat")
        override suspend fun deleteLlibre(id: Long) = error("no utilitzat")
        override suspend fun getLlibreById(id: Long) = error("no utilitzat")
        override suspend fun getAllAutors() = error("no utilitzat")
        override suspend fun addAutor(autor: Autor) = error("no utilitzat")
        override suspend fun deleteAutor(id: Long) = error("no utilitzat")
        override suspend fun getAllExemplars() = error("no utilitzat")
        override suspend fun getExemplarsLliures(titol: String?, autor: String?) = error("no utilitzat")
        override suspend fun addExemplar(exemplar: Exemplar) = error("no utilitzat")
        override suspend fun updateExemplar(id: Long, exemplar: Exemplar) = error("no utilitzat")
        override suspend fun deleteExemplar(id: Long) = error("no utilitzat")
        override suspend fun getExemplarById(id: Long) = error("no utilitzat")
    }

    // ========== TESTS ==========

    /**
     * Test: Càrrega de préstecs actius amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar préstecs actius correctament des de l'API falsa
     * - Que l'estat `isLoading` es gestiona correctament
     * - Que la llista de préstecs actius es carrega correctament
     * - Que tots els préstecs retornats són actius (dataDevolucio == null)
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeApiSuccess que retorna 1 préstec actiu inicial
     * 
     * **Resultats esperats:**
     * - isLoading == false
     * - loans.isNotEmpty() == true
     * - Tots els préstecs tenen dataDevolucio == null
     * - error == null
     */
    @Test
    fun loadActiveLoans_exitos_retornaLlista() = runTest {
        val vm = LoanViewModel(api = FakeApiSuccess())
        vm.loadActiveLoans()
        advanceUntilIdle()

        val state = vm.activeLoansState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir préstecs actius", state.loans.isNotEmpty())
        assertTrue("Tots els préstecs haurien d'estar actius",
            state.loans.all { it.dataDevolucio == null })
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Càrrega de préstecs actius per usuari específic amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar préstecs actius d'un usuari específic correctament
     * - Que l'estat `isLoading` es gestiona correctament
     * - Que tots els préstecs retornats pertanyen a l'usuari especificat
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeApiSuccess que filtra préstecs per usuariId
     * - Es carreguen préstecs actius per usuariId=1L
     * 
     * **Resultats esperats:**
     * - isLoading == false
     * - Tots els préstecs tenen usuari?.id == 1L
     * - error == null
     */
    @Test
    fun loadActiveLoans_perUsuari_retornaNomésPrestecsUsuari() = runTest {
        val vm = LoanViewModel(api = FakeApiSuccess())
        vm.loadActiveLoans(usuariId = 1L)
        advanceUntilIdle()

        val state = vm.activeLoansState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Tots els préstecs haurien de ser de l'usuari 1",
            state.loans.all { it.usuari?.id == 1L })
    }

    /**
     * Test: Càrrega de préstecs actius amb error.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament els errors quan l'API falla
     * - Que l'estat `isLoading` es gestiona correctament (finalitza a false)
     * - Que l'estat `error` conté el missatge d'error
     * - Que la llista de préstecs no es carrega quan hi ha error
     * 
     * **Condicions:**
     * - S'utilitza FakeApiError que llança Exception("Error de xarxa")
     * 
     * **Resultats esperats:**
     * - isLoading == false
     * - error != null
     * - error.contains("Error") == true
     */
    @Test
    fun loadActiveLoans_error_mostraMissatgeError() = runTest {
        val vm = LoanViewModel(api = FakeApiError())
        vm.loadActiveLoans()
        advanceUntilIdle()

        val state = vm.activeLoansState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertNotNull("Hauria de tenir error", state.error)
        assertTrue("Error hauria de contenir 'Error'", state.error!!.contains("Error"))
    }

    /**
     * Test: Càrrega d'historial de préstecs amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar l'historial complet de préstecs correctament
     * - Que l'estat `isLoading` es gestiona correctament
     * - Que la llista d'historial es carrega correctament
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeApiSuccess que retorna tots els préstecs (actius i retornats)
     * 
     * **Resultats esperats:**
     * - isLoading == false
     * - loans.isNotEmpty() == true
     * - error == null
     */
    @Test
    fun loadLoanHistory_exitos_retornaLlista() = runTest {
        val vm = LoanViewModel(api = FakeApiSuccess())
        vm.loadLoanHistory()
        advanceUntilIdle()

        val state = vm.loanHistoryState.value
        assertFalse("No hauria d'estar carregant", state.isLoading)
        assertTrue("Hauria de tenir préstecs", state.loans.isNotEmpty())
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Creació de préstec amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot crear un nou préstec correctament
     * - Que l'estat `isSubmitting` es gestiona correctament
     * - Que l'estat `success` es posa a true després de crear
     * - Que el préstec creat està disponible a `createdLoan`
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeApiSuccess que gestiona la creació internament
     * - Es crea un préstec per usuariId=2L i exemplarId=2L
     * 
     * **Resultats esperats:**
     * - isSubmitting == false
     * - success == true
     * - createdLoan != null
     * - error == null
     */
    @androidx.test.filters.SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun createLoan_exitos_creaPrestec() = runTest {
        val vm = LoanViewModel(api = FakeApiSuccess())
        vm.createLoan(
            usuariId = 2L,
            exemplarId = 2L,
            dataPrestec = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        advanceUntilIdle()

        val state = vm.createLoanState.value
        assertFalse("No hauria d'estar enviant", state.isSubmitting)
        assertTrue("Hauria de ser exitós", state.success)
        assertNotNull("Hauria de tenir préstec creat", state.createdLoan)
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Creació de préstec amb error.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament els errors quan la creació falla
     * - Que l'estat `isSubmitting` es gestiona correctament (finalitza a false)
     * - Que l'estat `success` es posa a false
     * - Que l'estat `error` conté el missatge d'error
     * 
     * **Condicions:**
     * - S'utilitza FakeApiError que llança Exception("Error creant préstec")
     * 
     * **Resultats esperats:**
     * - isSubmitting == false
     * - success == false
     * - error != null
     */
    @androidx.test.filters.SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun createLoan_error_mostraMissatgeError() = runTest {
        val vm = LoanViewModel(api = FakeApiError())
        vm.createLoan(
            usuariId = 1L,
            exemplarId = 1L
        )
        advanceUntilIdle()

        val state = vm.createLoanState.value
        assertFalse("No hauria d'estar enviant", state.isSubmitting)
        assertFalse("No hauria de ser exitós", state.success)
        assertNotNull("Hauria de tenir error", state.error)
    }

    /**
     * Test: Devolució de préstec amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot retornar un préstec correctament
     * - Que l'estat `isReturning` es gestiona correctament
     * - Que l'estat `success` es posa a true després de retornar
     * - Que l'ID del préstec retornat està disponible a `returnedLoanId`
     * - Que no hi ha errors en la operació
     * 
     * **Condicions:**
     * - S'utilitza FakeApiSuccess que gestiona la devolució internament
     * - Es carrega un préstec actiu inicial
     * - Es retorna el préstec amb l'ID corresponent
     * 
     * **Resultats esperats:**
     * - isReturning == null
     * - success == true
     * - returnedLoanId == prestecId
     * - error == null
     */
    @Test
    fun returnLoan_exitos_retornaPrestec() = runTest {
        val vm = LoanViewModel(api = FakeApiSuccess())
        vm.loadActiveLoans()
        advanceUntilIdle()

        val prestecId = vm.activeLoansState.value.loans.first().id!!
        vm.returnLoan(prestecId)
        advanceUntilIdle()

        val state = vm.returnLoanState.value
        assertNull("No hauria d'estar retornant", state.isReturning)
        assertTrue("Hauria de ser exitós", state.success)
        assertEquals("Hauria de retornar el préstec correcte", prestecId, state.returnedLoanId)
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Devolució de préstec amb error.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel gestiona correctament els errors quan la devolució falla
     * - Que l'estat `isReturning` es gestiona correctament (finalitza a null)
     * - Que l'estat `success` es posa a false
     * - Que l'estat `error` conté el missatge d'error
     * 
     * **Condicions:**
     * - S'utilitza FakeApiError que retorna Response.error(404)
     * - Es intenta retornar un préstec amb id=999L (no existeix)
     * 
     * **Resultats esperats:**
     * - isReturning == null
     * - success == false
     * - error != null
     */
    @Test
    fun returnLoan_error_mostraMissatgeError() = runTest {
        val vm = LoanViewModel(api = FakeApiError())
        vm.returnLoan(999L)
        advanceUntilIdle()

        val state = vm.returnLoanState.value
        assertNull("No hauria d'estar retornant", state.isReturning)
        assertFalse("No hauria de ser exitós", state.success)
        assertNotNull("Hauria de tenir error", state.error)
    }

    /**
     * Test: Neteja d'errors.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot netejar errors correctament
     * - Que després de netejar, l'estat `error` es posa a null
     * - Que els errors es poden netejar de manera independent
     * 
     * **Condicions:**
     * - S'utilitza FakeApiError que genera un error
     * - Es carreguen préstecs actius (genera error)
     * - Es netegen els errors
     * 
     * **Resultats esperats:**
     * - Abans de netejar: error != null
     * - Després de netejar: error == null
     */
    @Test
    fun clearErrors_netejaErrors() = runTest {
        val vm = LoanViewModel(api = FakeApiError())
        vm.loadActiveLoans()
        advanceUntilIdle()

        // Verificar que hi ha error
        assertNotNull("Hauria de tenir error", vm.activeLoansState.value.error)

        // Netejar errors
        vm.clearErrors()

        // Verificar que s'han netejat
        assertNull("Error hauria d'haver estat netejat", vm.activeLoansState.value.error)
    }

    /**
     * Test: Reinici de formularis.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot reiniciar formularis correctament
     * - Que després de reiniciar, els estats es posen als valors inicials
     * - Que els errors es netegen
     * - Que els estats de creació es reinicien
     * 
     * **Condicions:**
     * - S'utilitza FakeApiSuccess
     * - Es modifiquen estats de creació (isSubmitting=true, error="Error de test")
     * - Es reinicien els formularis
     * 
     * **Resultats esperats:**
     * - isSubmitting == false
     * - success == false
     * - error == null
     */
    @Test
    fun resetForms_reiniciaFormularis() = runTest {
        val vm = LoanViewModel(api = FakeApiSuccess())
        
        // Modificar estats
        vm._createLoanState.value = vm.createLoanState.value.copy(
            isSubmitting = true,
            error = "Error de test"
        )

        // Reiniciar formularis
        vm.resetForms()

        // Verificar que s'han reiniciat
        val createState = vm.createLoanState.value
        assertFalse("No hauria d'estar enviant", createState.isSubmitting)
        assertFalse("No hauria de ser exitós", createState.success)
        assertNull("No hauria d'haver error", createState.error)
    }
}






