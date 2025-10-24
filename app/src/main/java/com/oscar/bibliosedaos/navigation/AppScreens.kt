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
 * @see androidx.navigation.NavController
 * @see MainActivity.AppNavigation
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
     * @see AddUserScreen
     * @see CreateUserRequest
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
     * @since 1.0
     * @see ProfileScreen
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
     * @see ProfileScreen
     * @see EditProfileScreen
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
     * **Camps Editables:**
     * - ✅ Nick (amb validació d'unicitat)
     * - ✅ Nombre
     * - ✅ Primer cognom (apellido1)
     * - ✅ Segon cognom (apellido2) - Opcional
     *
     * **Camps NO Editables:**
     * -  ID d'usuari (permanent i immutable)
     * -  Rol (requereix permisos especials)
     * -  Contrasenya (pantalla separada planificada)
     *
     * **Funcionalitats:**
     * - Detecció automàtica de canvis sense guardar
     * - Validació en temps real de cada camp
     * - Advertència visual si hi ha canvis pendents
     * - Botó "Guardar" deshabilitat si hi ha errors
     * - Confirmació visual després de guardar
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
}