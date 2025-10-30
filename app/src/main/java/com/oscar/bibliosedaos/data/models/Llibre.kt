package com.oscar.bibliosedaos.data.models

import com.google.gson.annotations.SerializedName

/**
 * Model de dades per representar un llibre a la biblioteca.
 *
 * Correspon a l'entitat Llibre del backend Spring Boot.
 * Inclou tota la informació bibliogràfica necessària per identificar
 * i gestionar un llibre dins del sistema.
 *
 * @property id Identificador únic del llibre
 * @property isbn Codi ISBN únic del llibre (International Standard Book Number)
 * @property titol Títol complet del llibre
 * @property pagines Nombre total de pàgines del llibre
 * @property editorial Nom de l'editorial que va publicar el llibre
 * @property autor Objecte Autor que representa l'autor del llibre
 *
 * @author Oscar
 * @since 1.0
 * @see Autor
 * @see Exemplar
 */
data class Llibre(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("isbn")
    val isbn: String,

    @SerializedName("titol")
    val titol: String,

    @SerializedName("pagines")
    val pagines: Int,

    @SerializedName("editorial")
    val editorial: String,

    @SerializedName("autor")
    val autor: Autor? = null
)