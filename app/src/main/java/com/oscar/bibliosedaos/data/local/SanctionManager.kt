package com.oscar.bibliosedaos.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestor de sancions per a usuaris.
 * 
 * **Funcionalitat:**
 * - Guarda les sancions localment utilitzant SharedPreferences
 * - Permet aplicar sancions amb durada personalitzada
 * - Comprova si un usuari està sancionat
 * - Gestiona l'expiració de sancions
 * 
 * **Nota:** Aquest sistema funciona només al frontend. Per a una implementació
 * completa, caldria afegir suport al backend.
 * 
 * @author Oscar
 * @since 1.0
 */
object SanctionManager {
    private const val PREFS_NAME = "sanctions_prefs"
    private const val KEY_SANCTIONS = "sanctions"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Obté les SharedPreferences per a les sancions.
     * Utilitza sempre el context de l'aplicació per assegurar que és el mateix fitxer.
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        val appContext = if (context is android.app.Application) {
            context
        } else {
            context.applicationContext
        }
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Aplica una sanció a un usuari.
     * 
     * @param context Context de l'aplicació
     * @param userId ID de l'usuari a sancionar
     * @param reason Motiu de la sanció
     * @param durationDays Durada de la sanció en dies (null = fins que retorni tots els llibres)
     * @param adminId ID de l'administrador que aplica la sanció
     */
    fun applySanction(
        context: Context,
        userId: Long,
        reason: String,
        durationDays: Int?,
        adminId: Long
    ) {
        val prefs = getSharedPreferences(context)
        val gson = Gson()
        val sanctionsJson = prefs.getString(KEY_SANCTIONS, "[]")
        
        val type = object : TypeToken<List<Sanction>>() {}.type
        val sanctions = try {
            gson.fromJson<List<Sanction>>(sanctionsJson, type)?.toMutableList() ?: mutableListOf()
        } catch (e: Exception) {
            Log.e("SanctionManager", "Error al llegir sancions: ${e.message}", e)
            mutableListOf()
        }

        // Eliminar sancions antigues d'aquest usuari
        sanctions.removeAll { it.userId == userId }

        // Calcular data d'expiració
        val expirationDate = if (durationDays != null) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, durationDays)
            dateFormat.format(calendar.time)
        } else {
            null // Fins que retorni tots els llibres
        }

        // Crear nova sanció
        val sanction = Sanction(
            userId = userId,
            reason = reason,
            appliedDate = dateFormat.format(Date()),
            expirationDate = expirationDate,
            appliedBy = adminId,
            isActive = true
        )

        sanctions.add(sanction)

        // Guardar
        val editor = prefs.edit()
        editor.putString(KEY_SANCTIONS, gson.toJson(sanctions))
        editor.commit() // Usar commit() per assegurar que es guarda immediatament
    }

    /**
     * Elimina una sanció d'un usuari.
     * 
     * @param context Context de l'aplicació
     * @param userId ID de l'usuari
     */
    fun removeSanction(context: Context, userId: Long) {
        val prefs = getSharedPreferences(context)
        val gson = Gson()
        val sanctionsJson = prefs.getString(KEY_SANCTIONS, "[]")
        val type = object : TypeToken<List<Sanction>>() {}.type
        val sanctions = gson.fromJson<List<Sanction>>(sanctionsJson, type)?.toMutableList() ?: mutableListOf()

        sanctions.removeAll { it.userId == userId }

        val editor = prefs.edit()
        editor.putString(KEY_SANCTIONS, gson.toJson(sanctions))
        editor.commit() // Usar commit() per assegurar que es guarda immediatament
    }

    /**
     * Comprova si un usuari està sancionat.
     * 
     * @param context Context de l'aplicació
     * @param userId ID de l'usuari
     * @return La sanció activa si existeix, null altrament
     */
    fun isUserSanctioned(context: Context, userId: Long): Sanction? {
        val prefs = getSharedPreferences(context)
        val gson = Gson()
        val sanctionsJson = prefs.getString(KEY_SANCTIONS, "[]")
        
        val type = object : TypeToken<List<Sanction>>() {}.type
        val sanctions = try {
            gson.fromJson<List<Sanction>>(sanctionsJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("SanctionManager", "Error al llegir sancions: ${e.message}", e)
            emptyList()
        }

        val today = dateFormat.format(Date())

        // Netejar sancions expirades abans de comprovar
        val activeSanctions = sanctions.filter { sanction ->
            sanction.isActive && (
                sanction.expirationDate == null || // Fins que retorni tots els llibres
                sanction.expirationDate >= today // Encara no ha expirat
            )
        }

        return activeSanctions.firstOrNull { it.userId == userId }
    }

    /**
     * Obté totes les sancions actives.
     * 
     * @param context Context de l'aplicació
     * @return Llista de sancions actives
     */
    fun getAllActiveSanctions(context: Context): List<Sanction> {
        val prefs = getSharedPreferences(context)
        val gson = Gson()
        val sanctionsJson = prefs.getString(KEY_SANCTIONS, "[]")
        val type = object : TypeToken<List<Sanction>>() {}.type
        val sanctions = gson.fromJson<List<Sanction>>(sanctionsJson, type) ?: emptyList()

        val today = dateFormat.format(Date())

        return sanctions.filter { sanction ->
            sanction.isActive && (
                sanction.expirationDate == null ||
                sanction.expirationDate >= today
            )
        }
    }

    /**
     * Obté la sanció d'un usuari específic.
     * 
     * @param context Context de l'aplicació
     * @param userId ID de l'usuari
     * @return La sanció si existeix, null altrament
     */
    fun getUserSanction(context: Context, userId: Long): Sanction? {
        return isUserSanctioned(context, userId)
    }

    /**
     * Neteja les sancions expirades.
     * 
     * @param context Context de l'aplicació
     */
    fun cleanExpiredSanctions(context: Context) {
        val prefs = getSharedPreferences(context)
        val gson = Gson()
        val sanctionsJson = prefs.getString(KEY_SANCTIONS, "[]")
        val type = object : TypeToken<List<Sanction>>() {}.type
        val sanctions = gson.fromJson<List<Sanction>>(sanctionsJson, type)?.toMutableList() ?: mutableListOf()

        val today = dateFormat.format(Date())

        sanctions.removeAll { sanction ->
            sanction.expirationDate != null && sanction.expirationDate < today
        }

        val editor = prefs.edit()
        editor.putString(KEY_SANCTIONS, gson.toJson(sanctions))
        editor.commit() // Usar commit() per assegurar que es guarda immediatament
    }
}

/**
 * Model de dades per a una sanció.
 * 
 * @property userId ID de l'usuari sancionat
 * @property reason Motiu de la sanció
 * @property appliedDate Data en què es va aplicar la sanció (format: "yyyy-MM-dd")
 * @property expirationDate Data d'expiració de la sanció (format: "yyyy-MM-dd", null = fins que retorni tots els llibres)
 * @property appliedBy ID de l'administrador que va aplicar la sanció
 * @property isActive Si la sanció està activa
 */
data class Sanction(
    val userId: Long,
    val reason: String,
    val appliedDate: String,
    val expirationDate: String?,
    val appliedBy: Long,
    val isActive: Boolean = true
) {
    /**
     * Comprova si la sanció ha expirat.
     */
    fun isExpired(): Boolean {
        if (expirationDate == null) return false // Fins que retorni tots els llibres
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return expirationDate < today
    }

    /**
     * Obté els dies restants de la sanció.
     * 
     * @return Dies restants (null si no té data d'expiració)
     */
    fun getDaysRemaining(): Int? {
        if (expirationDate == null) return null
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val expiration = dateFormat.parse(expirationDate) ?: return null
            val today = Date()
            val diffInMillis = expiration.time - today.time
            val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
            maxOf(0, diffInDays)
        } catch (e: Exception) {
            null
        }
    }
}

