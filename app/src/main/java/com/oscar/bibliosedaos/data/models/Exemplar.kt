package com.oscar.bibliosedaos.data.models

import com.google.gson.annotations.SerializedName

/**
 * Model de dades per representar un exemplar físic d'un llibre.
 *
 * Correspon a l'entitat Exemplar del backend Spring Boot.
 * Cada llibre pot tenir múltiples exemplars físics a la biblioteca,
 * cadascun amb la seva pròpia ubicació i estat de disponibilitat.
 *
 * @property id Identificador únic de l'exemplar
 * @property lloc Ubicació física de l'exemplar a la biblioteca (prestatgeria, secció, etc.)
 * @property reservat Estat de disponibilitat: "lliure", "prestat", "reservat"
 * @property llibre Objecte Llibre associat a aquest exemplar
 *
 * @author Oscar
 * @since 1.0
 */
data class Exemplar(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("lloc")
    val lloc: String,

    @SerializedName("reservat")
    val reservat: String = "lliure", // "lliure", "prestat", "reservat"

    @SerializedName("llibre")
    val llibre: Llibre? = null
)

