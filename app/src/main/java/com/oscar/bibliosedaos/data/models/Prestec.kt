package com.oscar.bibliosedaos.data.models

import com.google.gson.annotations.SerializedName

/**
 * Model de dades per representar un préstec de llibre.
 *
 * Correspon a l'entitat Prestec del backend Spring Boot.
 * Representa el préstec d'un exemplar físic a un usuari,
 * incloent les dates de préstec i devolució.
 *
 * @property id Identificador únic del préstec
 * @property dataPrestec Data en què es va fer el préstec (LocalDate)
 * @property dataDevolucio Data de devolució real (null si encara està actiu)
 * @property usuari Usuari que té el llibre prestat
 * @property exemplar Exemplar físic que s'ha prestat
 *
 * @author Oscar
 * @since 1.0
 * @see Exemplar
 * @see com.oscar.bibliosedaos.data.network.User
 */
data class Prestec(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("dataPrestec")
    val dataPrestec: String, // Format: "yyyy-MM-dd"

    @SerializedName("dataDevolucio")
    val dataDevolucio: String? = null, // null si el préstec està actiu

    @SerializedName("usuari")
    val usuari: com.oscar.bibliosedaos.data.network.User? = null,

    @SerializedName("exemplar")
    val exemplar: Exemplar
) {
    /**
     * Comprova si el préstec està actiu (no s'ha retornat).
     *
     * @return true si dataDevolucio és null, false altrament
     */
    val isActive: Boolean
        get() = dataDevolucio == null

    /**
     * Obté el títol del llibre prestat.
     *
     * @return Títol del llibre o "Desconegut" si no està disponible
     */
    val bookTitle: String
        get() = exemplar.llibre?.titol ?: "Desconegut"

    /**
     * Obté el nom de l'autor del llibre prestat.
     *
     * @return Nom de l'autor o "Desconegut" si no està disponible
     */
    val authorName: String
        get() = exemplar.llibre?.autor?.nom ?: "Desconegut"

    /**
     * Obté la ubicació de l'exemplar a la biblioteca.
     *
     * @return Ubicació de l'exemplar
     */
    val location: String
        get() = exemplar.lloc
}

/**
 * Request per crear un nou préstec.
 *
 * S'utilitza quan l'usuari vol prestar un llibre.
 * Només l'administrador pot crear préstecs.
 *
 * @property dataPrestec Data del préstec (format: "yyyy-MM-dd")
 * @property usuari Objecte User amb l'ID de l'usuari
 * @property exemplar Objecte Exemplar amb l'ID de l'exemplar
 *
 * @author Oscar
 * @since 1.0
 */
data class CreatePrestecRequest(
    @SerializedName("dataPrestec")
    val dataPrestec: String,

    @SerializedName("usuari")
    val usuari: UserIdOnly,

    @SerializedName("exemplar")
    val exemplar: ExemplarIdOnly
)

/**
 * Classe auxiliar per enviar només l'ID de l'usuari.
 */
data class UserIdOnly(
    @SerializedName("id")
    val id: Long
)

/**
 * Classe auxiliar per enviar només l'ID de l'exemplar.
 */
data class ExemplarIdOnly(
    @SerializedName("id")
    val id: Long
)