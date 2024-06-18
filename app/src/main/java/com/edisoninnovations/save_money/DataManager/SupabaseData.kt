package com.edisoninnovations.save_money.DataManager

import com.edisoninnovations.save_money.models.Transaction
import com.edisoninnovations.save_money.supabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun obtenerTransacciones(fecha: String, idUsuario: String): List<Transaction>? {
    return try {
        if (fecha.isNotBlank() && idUsuario.isNotBlank()) {

            val response = withContext(Dispatchers.IO) {
                supabase.postgrest.rpc("get_transactions_by_date_and_user", mapOf(
                    "p_fecha" to fecha,
                    "p_id_usuario" to idUsuario
                ))
            }

            println("##########################################Obteniedo dartos HomeInformation"+response.data)

            // Utilizar Moshi para parsear la respuesta JSON
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, Map::class.java)
            val jsonAdapter = moshi.adapter<List<Map<String, Any>>>(type)
            val parsedData = jsonAdapter.fromJson(response.data.toString())

            parsedData?.map {

                Transaction(
                    id_transaccion = (it["id_transaccion"] as? Double)?.toInt().toString(),  // Convertir a Int y luego a String
                    category = it["nombre_categoria"].toString(),
                    amount = it["cantidad"].toString().toDouble(),
                    note = it["nota"].toString(),
                    tipo = it["tipo"].toString(),
                    imageUrls = null, // Placeholder, we'll fetch images later
                    id_account = it["id_account"].toString(),
                    title = it["title"].toString()
                )
            }

        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        println("########################################################################error: " + e.message)
        null
    }
}

suspend fun obtenerImagenesPorTransacciones(idsTransacciones: List<String>): Map<String, List<String>>? {
    return try {
        val response = withContext(Dispatchers.IO) {
            supabase.from("transimages").select(columns = Columns.list("id_transaccion", "imagen")) {
                filter {
                    isIn("id_transaccion", idsTransacciones)
                }
            }
        }
        // Utilizar Moshi para parsear la respuesta JSON
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, Map::class.java)
        val jsonAdapter = moshi.adapter<List<Map<String, Any>>>(type)
        val data = jsonAdapter.fromJson(response.data.toString()) ?: return null

        val imagenesPorTransaccion = mutableMapOf<String, MutableList<String>>()

        data.forEach { item ->
            val idTransaccion = (item["id_transaccion"] as? Double)?.toInt().toString()
            val imagen = item["imagen"].toString()

            if (!imagenesPorTransaccion.containsKey(idTransaccion)) {
                imagenesPorTransaccion[idTransaccion] = mutableListOf()
            }
            imagenesPorTransaccion[idTransaccion]?.add(imagen)
        }
        imagenesPorTransaccion
    } catch (e: Exception) {
        e.printStackTrace()
        println("########################################################################error: " + e.message)

        null
    }
}

suspend fun insertarTransaccion(
    idCategoria: Int,
    nota: String?,
    tipo: String,
    cantidad: Double,
    idUsuario: String,
    fecha: String
): String? {
    return try {
        val transaccionData = mapOf(
            "id_categoria" to idCategoria,
            "nota" to nota,
            "tipo" to tipo,
            "cantidad" to cantidad,
            "id_usuario" to idUsuario,
            "fecha" to fecha,
            "tiempo" to System.currentTimeMillis()
        )

        val response = withContext(Dispatchers.IO) {
            supabase.from("transacciones").insert(transaccionData)
        }

        response.data?.let {
            val insertedId = "insertado"
            insertedId
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
