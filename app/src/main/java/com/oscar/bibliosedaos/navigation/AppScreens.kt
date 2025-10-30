package com.oscar.bibliosedaos.navigation

/**
 * Definició de totes les rutes de navegació de l'aplicació.
 *
 * Aquesta classe sealed encapsula totes les rutes disponibles a l'app,
 * facilitant la navegació type-safe amb Jetpack Compose Navigation.
 * Cada objecte representa una pantalla única de l'aplicació.
 *
 *
 * @property route String que identifica únicament la ruta de navegació
 *
 * @author Oscar
 * @since 1.0
 */
sealed class AppScreens(val route: String) {

    /**
     * Pantalla d'inici de sessió.
     *
     * **Descripció:**
     * Pantalla inicial de l'aplicació on els usuaris introdueixen les seves
     * credencials (nick i password) per autenticar-se al sistema.
     *
     *
     * @author Oscar
     * @since 1.0
     * @see LoginScreen
     */
    object LoginScreen : AppScreens("login_screen")

    // ========== PANTALLAS DE ADMINISTRADOR ==========

    /**
     * Pantalla principal de l'administrador.
     *
     * **Descripció:**
     * Pantalla de gestió d'usuaris exclusiva per administradors. Mostra
     * la llista completa d'usuaris registrats amb opcions de gestió CRUD.
     *
     * **Funcionalitats:**
     * - Llistar tots els usuaris del sistema
     * - Veure perfil de qualsevol usuari (clic a card)
     * - Eliminar usuaris (excepte a si mateix)
     * - Botó FAB per afegir nous usuaris
     * - Cerca i filtrat d'usuaris (futur)
     *
     * **Requeriments:**
     * -  Només accessible amb rol d'administrador (rol=2)
     * -  Requereix token JWT vàlid
     *
     * @author Oscar
     * @since 1.0
     * @see AdminHomeScreen
     * @see AddUserScreen
     */
    object AdminHomeScreen : AppScreens("admin_home_screen")


    /**
     * Pantalla de cerca d'usuaris per ID o NIF.
     *
     * **Descripció:**
     * Pantalla dedicada que permet a l'administrador cercar usuaris
     * específics mitjançant el seu identificador únic (ID) o el seu
     * document d'identitat (NIF/DNI).
     *
     * **Funcionalitats:**
     * - Cerca per ID de l'usuari (número únic)
     * - Cerca per NIF/DNI (document d'identitat)
     * - Validació de dades introduïdes
     * - Visualització de resultats amb informació completa
     * - Navegació directa al perfil complet de l'usuari
     *
     * **Característiques:**
     * - No mostra usuaris fins que es realitzi una cerca activa
     * - Missatges d'error clars i descriptius
     * - Permet netejar la cerca fàcilment
     * - Pestanyes per seleccionar tipus de cerca
     *
     * **Requeriments:**
     * - ⚠️ Només accessible per administradors (rol=2)
     * - 🔒 Requereix token JWT vàlid
     *
     * **Navegació:**
     * - **Entrada:** Des de AdminHomeScreen (card "Cercar Usuari")
     * - **Sortida:** UserProfileScreen (quan es troba un usuari)
     *
     * **Ruta:** `user_search_screen`
     *
     * @author Oscar
     * @since 1.0
     * @see UserSearchScreen
     * @see AdminHomeScreen
     */
    object UserSearchScreen : AppScreens("user_search_screen")


    /**
     * Pantalla per afegir un nou usuari al sistema.
     *
     * **Descripció:**
     * Formulari complet per crear nous usuaris. Inclou validació en temps
     * real de tots els camps i assignació de rols.
     *
     * **Funcionalitats:**
     * - Formulari amb validació en temps real
     * - Camps obligatoris: nick, password, nombre, apellido1, rol
     * - Camps opcionals: apellido2, nif, localitat, etc.
     * - Confirmació de contrasenya
     * - Selecció de rol (Usuari Normal/Administrador)
     *
     * **Validacions:**
     * - Nick: 3-50 caràcters, alfanumèric + guió baix
     * - Password: mínim 6 caràcters
     * - Nombre: mínim 2 caràcters
     * - Les contrasenyes han de coincidir
     *
     * **Requeriments:**
     * - ️ Només accessible per administradors
     * - ️ Requereix token JWT vàlid
     *

     *
     * @author Oscar
     * @since 1.0
       */
    object AddUserScreen : AppScreens("add_user_screen")

    // ========== PANTALLAS DE USUARIO ==========

    /**
     * Pantalla principal de l'usuari normal.
     *
     * **Descripció:**
     * Pantalla principal per usuaris normals amb accés a funcionalitats
     * d'usuari com préstecs, reserves, resenyes, etc.
     *
     * **Funcionalitats Planificades:**
     * - Gestió de préstecs actius
     * - Reserves de llibres
     * - Historial de préstecs
     * - Sistema de resenyes
     * - Llibres favorits
     * - Notificacions
     *
     * **Requeriments:**
     * -  Accessible amb rol d'usuari normal (rol=1)
     * -  Requereix token JWT vàlid
     *
     * @author Oscar
         */
    object UserHomeScreen : AppScreens("user_home_screen")

    /**
     * Pantalla de perfil d'usuari (amb paràmetre dinàmic).
     *
     * **Descripció:**
     * Pantalla versàtil que mostra el perfil d'un usuari i s'adapta
     * dinàmicament segons el rol i si és el perfil propi o d'un altre usuari.
     *
     * **Modes d'Ús:**
     * 1. **Perfil Propi (Usuari):**
     *    - Mostra informació personal
     *    - Opcions de gestió de compte
     *    - Funcionalitats d'usuari (préstecs, reserves, etc.)
     *
     * 2. **Perfil Propi (Admin):**
     *    - Mostra informació personal
     *    - Opcions de gestió de compte
     *    - Accés a funcions administratives
     *    - Enllaç al panell web complet
     *
     * 3. **Perfil d'Altre Usuari (Admin):**
     *    - Visualització de dades de l'usuari
     *    - Botó "Volver" per tornar a la llista
     *    - Opcions d'edició (futur)
     *
     * **Detecció Automàtica:**
     * La pantalla detecta automàticament si l'usuari està veient el seu
     * propi perfil comparant l'ID del token amb l'ID de la ruta.
     *
     * **Paràmetres de Ruta:**
     * - `userId`: Identificador Long de l'usuari a mostrar
     *
     *
     * @author Oscar
     * @since 1.0
         */
    object UserProfileScreen : AppScreens("user_profile_screen/{userId}") {
        /**
         * Crea una ruta completa amb l'ID de l'usuari.
         *
         * Mètode d'utilitat per construir la ruta amb el paràmetre userId.
         * Substitueix el placeholder `{userId}` pel valor real.
         *
         *
         * @param userId Identificador Long de l'usuari
         * @return String amb la ruta completa (ex: "user_profile_screen/42")
         *
         * @author Oscar
         * @since 1.0
         */
        fun createRoute(userId: Long) = "user_profile_screen/$userId"
    }

    /**
     * Pantalla per editar el perfil de l'usuari actual.
     *
     * **Descripció:**
     * Formulari d'edició del perfil personal amb validació en temps real.
     * Permet modificar dades personals mantenint la integritat del sistema.
     *
     *
     * **Validacions:**
     * - Nick: 3-50 caràcters, alfanumèric + guió baix, únic
     * - Nombre: mínim 2 caràcters
     * - Apellido1: mínim 2 caràcters
     *
     * **Requeriments:**
     * -  Només pot editar el propi perfil
     * -  Requereix token JWT vàlid
     *
     * @author Oscar
     * @since 1.0
     * @see EditProfileScreen
     * @see User
     */
    object EditProfileScreen : AppScreens("edit_profile_screen")

    // ========== PANTALLAS COMPARTIDAS (FUTURO) ==========

    /**
     * Pantalla del catàleg de llibres.
     *
     * **Descripció:**
     * Pantalla compartida accessible per tots els usuaris autenticats
     * (administradors i usuaris normals). Mostra el catàleg complet de
     * llibres disponibles a la biblioteca.
     *
     * **Funcionalitats Planificades:**
     * - Llistat complet de llibres
     * - Cerca i filtrat per: títol, autor, ISBN, gènere, any
     * - Vista detallada de cada llibre
     * - Informació de disponibilitat
     * - Opcions segons rol:
     *   - **Usuari:** Reservar, valorar, afegir a favorits
     *   - **Admin:** CRUD de llibres, gestió d'inventari
     *
     * **Estat Actual:**
     *  **PENDENT D'IMPLEMENTACIÓ**
     * Actualment mostra un placeholder amb "Pantalla de Libros - Próximamente"
     *
     * **Requeriments:**
     * -  Accessible per usuaris i administradors
     * -  Requereix token JWT vàlid
     *
     * **Navegació:**
     * - **Entrada:** Des de ProfileScreen (qualsevol rol)
     * - **Sortida:** Book Detail Screen (futur)
     *
     * **Ruta:** `books_screen`
     *
     * @author Oscar
     * @since 1.0
     */
    object BooksScreen : AppScreens("books_screen")

    /**
     * Pantalla de canvi de contrassenya
     *
     */
    object ChangePasswordScreen : AppScreens("change_password")

    /**
     * Pantalla principal de gestió del catàleg de llibres.
     *
     * **Descripció:**
     * Interfície d'administrador per gestionar llibres, autors i exemplars.
     * Organitzada en tres pestanyes amb funcionalitats CRUD completes.
     *
     * **Funcionalitats:**
     * - Gestió de llibres (llistar, afegir, editar, eliminar)
     * - Gestió d'autors (llistar, afegir, eliminar)
     * - Gestió d'exemplars (llistar, afegir, cercar, eliminar)
     *
     * **Permisos:**
     * - ⚠️ Només accessible per administradors (rol=2)
     * - 🔑 Requereix token JWT vàlid
     *
     * **Ruta:** `book_management_screen`
     *
     * @author Oscar
     * @since 1.0
        */
    object BookManagementScreen : AppScreens("book_management_screen")

    /**
     * Pantalla per afegir un nou llibre.
     *
     * **Descripció:**
     * Formulari complet per crear nous llibres amb validació
     * en temps real i selecció d'autors.
     *
     * **Permisos:**
     * - ⚠️ Només accessible per administradors (rol=2)
     *
     * **Ruta:** `add_book_screen`
     *
     * @author Oscar
     * @since 1.0
     * @see AddBookScreen
     */
    object AddBookScreen : AppScreens("add_book_screen")

    /**
     * Pantalla per editar un llibre existent.
     *
     * **Descripció:**
     * Formulari per modificar les dades d'un llibre existent.
     * Carrega les dades actuals i permet actualitzar-les.
     *
     * **Paràmetres de Ruta:**
     * - `bookId`: Identificador Long del llibre a editar
     *
     * **Permisos:**
     * - ⚠️ Només accessible per administradors (rol=2)
     *
     * **Ruta:** `edit_book_screen/{bookId}`
     *
     * @author Oscar
     * @since 1.0
     * @see EditBookScreen
     */
    object EditBookScreen : AppScreens("edit_book_screen/{bookId}") {
        /**
         * Crea una ruta completa amb l'ID del llibre.
         *
         * @param bookId Identificador del llibre
         * @return String amb la ruta completa (ex: "edit_book_screen/42")
         */
        fun createRoute(bookId: Long) = "edit_book_screen/$bookId"
    }

    /**
     * Pantalla per afegir un nou exemplar.
     *
     * **Descripció:**
     * Formulari per crear nous exemplars físics de llibres existents
     * amb assignació d'ubicació a la biblioteca.
     *
     * **Permisos:**
     * - ⚠️ Només accessible per administradors (rol=2)
     *
     * **Ruta:** `add_exemplar_screen`
     *
     * @author Oscar
     * @since 1.0
     * @see AddExemplarScreen
     */
    object AddExemplarScreen : AppScreens("add_exemplar_screen")
}