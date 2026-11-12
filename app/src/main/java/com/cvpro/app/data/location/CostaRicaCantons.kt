package com.cvpro.app.data.location

import java.text.Normalizer
import java.util.Locale

/**
 * Cantón de Costa Rica para una ciudad, cuando el Geocoder de Android no lo trae.
 *
 * Para Costa Rica el Geocoder devuelve la ciudad o el distrito ("Ciudad Quesada") y la provincia
 * ("Alajuela"), pero deja vacío el cantón ("San Carlos"). En la mayoría de los cantones la
 * ciudad principal se llama igual que el cantón (Grecia, Liberia, Turrialba) y no hace falta
 * nada; esta tabla tiene solo los que se llaman distinto.
 *
 * La clave es "ciudad|provincia", porque hay nombres repetidos: San Pablo, San Rafael o San
 * Isidro existen en varias provincias y son cantones distintos.
 */
object CostaRicaCantons {

    /** El cantón de [city] en [province], o null si no está en la tabla. */
    fun cantonOf(city: String, province: String): String? = CANTONS["${normalize(city)}|${normalize(province)}"]

    /** Minúsculas, sin tildes y sin "Ciudad"/"Provincia de" sobrantes, para comparar. */
    private fun normalize(text: String): String {
        val plain = Normalizer.normalize(text, Normalizer.Form.NFD).replace(MARKS, "")
        return plain.lowercase(Locale.ROOT).replace(PROVINCE_PREFIX, "").trim()
    }

    private val MARKS = Regex("\\p{M}+")
    private val PROVINCE_PREFIX = Regex("^provincia de\\s+")

    private val CANTONS: Map<String, String> = mapOf(
        // San José
        "santiago|san jose" to "Puriscal",
        "san marcos|san jose" to "Tarrazú",
        "ciudad colon|san jose" to "Mora",
        "guadalupe|san jose" to "Goicoechea",
        "san isidro|san jose" to "Vázquez de Coronado",
        "san ignacio|san jose" to "Acosta",
        "san juan|san jose" to "Tibás",
        "san vicente|san jose" to "Moravia",
        "san pedro|san jose" to "Montes de Oca",
        "san pablo|san jose" to "Turrubares",
        "santa maria|san jose" to "Dota",
        "san isidro de el general|san jose" to "Pérez Zeledón",
        // Alajuela
        "ciudad quesada|alajuela" to "San Carlos",
        "quesada|alajuela" to "San Carlos",
        "san pedro|alajuela" to "Poás",
        "sarchi|alajuela" to "Sarchí",
        "sarchi norte|alajuela" to "Sarchí",
        "san rafael|alajuela" to "Guatuso",
        // Cartago
        "tres rios|cartago" to "La Unión",
        "juan vinas|cartago" to "Jiménez",
        "pacayas|cartago" to "Alvarado",
        "san rafael|cartago" to "Oreamuno",
        "tejar|cartago" to "El Guarco",
        "el tejar|cartago" to "El Guarco",
        // Heredia
        "san antonio|heredia" to "Belén",
        "san joaquin|heredia" to "Flores",
        "puerto viejo|heredia" to "Sarapiquí",
        // Guanacaste
        "filadelfia|guanacaste" to "Carrillo",
        "las juntas|guanacaste" to "Abangares",
        "carmona|guanacaste" to "Nandayure",
        // Puntarenas
        "miramar|puntarenas" to "Montes de Oro",
        "ciudad cortes|puntarenas" to "Osa",
        "san vito|puntarenas" to "Coto Brus",
        "ciudad neily|puntarenas" to "Corredores",
        "jaco|puntarenas" to "Garabito",
        "santa elena|puntarenas" to "Monteverde",
        // Limón
        "guapiles|limon" to "Pococí",
        "bribri|limon" to "Talamanca",
        "puerto limon|limon" to "Limón",
    )
}
