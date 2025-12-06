package com.oscar.bibliosedaos.data.models

import com.google.gson.annotations.SerializedName

/**
 * Model de dades per representar un horari de sala.
 *
 * Correspon a l'entitat Horari del backend Spring Boot.
 * S'utilitza per gestionar els horaris de les sales de la biblioteca
 * per als grups de lectura.
 *
 * @property id Identificador únic de l'horari
 * @property sala Nom de la sala on es realitza l'activitat
 * @property dia Dia de la setmana (ex: "Dilluns", "Dimarts", etc.)
 * @property hora Hora de l'activitat (ex: "10:00", "18:30", etc.)
 * @property estat Estat de l'horari: "reservat" o "lliure"
 *
 * @author Oscar
 * @since 1.0
 * @see Grup
 */
data class Horari(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("sala")
    val sala: String,

    @SerializedName("dia")
    val dia: String,

    @SerializedName("hora")
    val hora: String,

    @SerializedName("estat")
    val estat: String? = null
) {
    /**
     * Comprova si l'horari està disponible.
     *
     * @return `true` si l'estat és "lliure" o null, `false` si està "reservat" o "ocupat"
     */
    val isLliure: Boolean
        get() = estat == null || estat.lowercase() == "lliure"

    /**
     * Retorna l'horari en format llegible.
     *
     * @return String amb format "Dia - Hora (Sala)"
     */
    val displayText: String
        get() = "$dia - $hora ($sala)"
}

