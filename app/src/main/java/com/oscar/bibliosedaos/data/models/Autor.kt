package com.oscar.bibliosedaos.data.models

import com.google.gson.annotations.SerializedName

/**
 * Model de dades per representar un autor de llibres.
 *
 * Correspon a l'entitat Autor del backend Spring Boot.
 * S'utilitza per la serialització/deserialització JSON amb Gson.
 *
 * @property id Identificador únic de l'autor
 * @property nom Nom complet de l'autor
 *
 * @author Oscar
 * @since 1.0
 * @see Llibre
 */
data class Autor(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("nom")
    val nom: String
)