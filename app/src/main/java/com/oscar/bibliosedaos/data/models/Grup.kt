package com.oscar.bibliosedaos.data.models

import com.google.gson.annotations.SerializedName
import com.oscar.bibliosedaos.data.network.User

/**
 * Model de dades per representar un grup de lectura.
 *
 * Correspon a l'entitat Grup del backend Spring Boot.
 * Un grup de lectura té un administrador, un horari assignat,
 * i una llista de membres participants.
 *
 * @property id Identificador únic del grup
 * @property nom Nom del grup de lectura
 * @property tematica Temàtica o gènere literari del grup
 * @property administrador Usuari administrador del grup
 * @property horari Horari assignat al grup (sala, dia, hora)
 * @property membres Llista d'usuaris que formen part del grup
 *
 * @author Oscar
 * @since 1.0
 * @see Horari
 * @see User
 */
data class Grup(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("nom")
    val nom: String,

    @SerializedName("tematica")
    val tematica: String,

    @SerializedName("administrador")
    val administrador: User? = null,

    @SerializedName("horari")
    val horari: Horari? = null,

    @SerializedName("membres")
    val membres: List<User>? = null
) {
    /**
     * Retorna el nombre total de membres del grup (incloent l'administrador).
     *
     * @return Nombre de membres + 1 (administrador)
     */
    val totalMembres: Int
        get() = (membres?.size ?: 0) + if (administrador != null) 1 else 0

    /**
     * Comprova si un usuari és membre del grup.
     *
     * @param userId ID de l'usuari a comprovar
     * @return `true` si l'usuari és membre o administrador, `false` altrament
     */
    fun isMember(userId: Long): Boolean {
        return administrador?.id == userId || membres?.any { it.id == userId } == true
    }

    /**
     * Comprova si un usuari és l'administrador del grup.
     *
     * @param userId ID de l'usuari a comprovar
     * @return `true` si l'usuari és l'administrador, `false` altrament
     */
    fun isAdmin(userId: Long): Boolean {
        return administrador?.id == userId
    }
}

/**
 * Petició per crear un nou grup de lectura.
 *
 * @property nom Nom del grup
 * @property tematica Temàtica del grup
 * @property administradorId ID de l'usuari administrador
 * @property horariId ID de l'horari assignat
 */
data class CreateGrupRequest(
    @SerializedName("nom")
    val nom: String,

    @SerializedName("tematica")
    val tematica: String,

    @SerializedName("administradorId")
    val administradorId: Long,

    @SerializedName("horariId")
    val horariId: Long
)

/**
 * Petició per actualitzar un grup existent.
 *
 * @property id ID del grup a actualitzar
 * @property nom Nom del grup
 * @property tematica Temàtica del grup
 * @property horariId ID de l'horari assignat (opcional)
 */
data class UpdateGrupRequest(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nom")
    val nom: String,

    @SerializedName("tematica")
    val tematica: String,

    @SerializedName("horariId")
    val horariId: Long? = null
)

/**
 * Petició per afegir un membre a un grup.
 *
 * @property grupId ID del grup
 * @property usuariId ID de l'usuari a afegir
 */
data class AddMemberRequest(
    @SerializedName("grupId")
    val grupId: Long,

    @SerializedName("usuariId")
    val usuariId: Long
)






