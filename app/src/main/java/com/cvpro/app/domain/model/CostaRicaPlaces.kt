package com.cvpro.app.domain.model

import java.text.Normalizer
import java.util.Locale

/**
 * Lugares de Costa Rica para sugerir mientras se escribe la ubicación (perfil y editor de CV).
 *
 * Son los 84 cantones con su provincia ("San Carlos, Alajuela") y las ciudades que se llaman
 * distinto a su cantón ("Ciudad Quesada, San Carlos, Alajuela"). La lista vive en la app: no
 * hace falta internet ni una API de mapas para sugerir, y el texto sale con el mismo formato
 * que "Usar mi ubicación".
 */
object CostaRicaPlaces {

    private val CANTONS: Map<String, List<String>> = mapOf(
        "San José" to listOf(
            "San José", "Escazú", "Desamparados", "Puriscal", "Tarrazú", "Aserrí", "Mora", "Goicoechea",
            "Santa Ana", "Alajuelita", "Vázquez de Coronado", "Acosta", "Tibás", "Moravia", "Montes de Oca",
            "Turrubares", "Dota", "Curridabat", "Pérez Zeledón", "León Cortés Castro",
        ),
        "Alajuela" to listOf(
            "Alajuela", "San Ramón", "Grecia", "San Mateo", "Atenas", "Naranjo", "Palmares", "Poás", "Orotina",
            "San Carlos", "Zarcero", "Sarchí", "Upala", "Los Chiles", "Guatuso", "Río Cuarto",
        ),
        "Cartago" to listOf(
            "Cartago", "Paraíso", "La Unión", "Jiménez", "Turrialba", "Alvarado", "Oreamuno", "El Guarco",
        ),
        "Heredia" to listOf(
            "Heredia", "Barva", "Santo Domingo", "Santa Bárbara", "San Rafael", "San Isidro", "Belén", "Flores",
            "San Pablo", "Sarapiquí",
        ),
        "Guanacaste" to listOf(
            "Liberia", "Nicoya", "Santa Cruz", "Bagaces", "Carrillo", "Cañas", "Abangares", "Tilarán", "Nandayure",
            "La Cruz", "Hojancha",
        ),
        "Puntarenas" to listOf(
            "Puntarenas", "Esparza", "Buenos Aires", "Montes de Oro", "Osa", "Quepos", "Golfito", "Coto Brus",
            "Parrita", "Corredores", "Garabito", "Monteverde", "Puerto Jiménez",
        ),
        "Limón" to listOf("Limón", "Pococí", "Siquirres", "Talamanca", "Matina", "Guácimo"),
    )

    /** Ciudades conocidas cuyo nombre no es el de su cantón: (ciudad, cantón, provincia). */
    private val CITIES: List<Triple<String, String, String>> = listOf(
        Triple("Ciudad Quesada", "San Carlos", "Alajuela"),
        Triple("Fortuna", "San Carlos", "Alajuela"),
        Triple("Aguas Zarcas", "San Carlos", "Alajuela"),
        Triple("Pital", "San Carlos", "Alajuela"),
        Triple("Florencia", "San Carlos", "Alajuela"),
        Triple("San Isidro de El General", "Pérez Zeledón", "San José"),
        Triple("Guadalupe", "Goicoechea", "San José"),
        Triple("San Pedro", "Montes de Oca", "San José"),
        Triple("San Juan", "Tibás", "San José"),
        Triple("San Vicente", "Moravia", "San José"),
        Triple("Ciudad Colón", "Mora", "San José"),
        Triple("Santiago", "Puriscal", "San José"),
        Triple("San Marcos", "Tarrazú", "San José"),
        Triple("Tres Ríos", "La Unión", "Cartago"),
        Triple("Juan Viñas", "Jiménez", "Cartago"),
        Triple("Pacayas", "Alvarado", "Cartago"),
        Triple("Tejar", "El Guarco", "Cartago"),
        Triple("San Antonio", "Belén", "Heredia"),
        Triple("San Joaquín", "Flores", "Heredia"),
        Triple("Puerto Viejo", "Sarapiquí", "Heredia"),
        Triple("Filadelfia", "Carrillo", "Guanacaste"),
        Triple("Tamarindo", "Santa Cruz", "Guanacaste"),
        Triple("Las Juntas", "Abangares", "Guanacaste"),
        Triple("Miramar", "Montes de Oro", "Puntarenas"),
        Triple("Ciudad Cortés", "Osa", "Puntarenas"),
        Triple("San Vito", "Coto Brus", "Puntarenas"),
        Triple("Ciudad Neily", "Corredores", "Puntarenas"),
        Triple("Jacó", "Garabito", "Puntarenas"),
        Triple("Santa Elena", "Monteverde", "Puntarenas"),
        Triple("Guápiles", "Pococí", "Limón"),
        Triple("Bribri", "Talamanca", "Limón"),
        Triple("Puerto Viejo de Talamanca", "Talamanca", "Limón"),
    )

    /** Todos los lugares, ya con el formato que se guarda. */
    val all: List<String> = buildList {
        CANTONS.forEach { (province, cantons) ->
            cantons.forEach { canton -> add(if (canton == province) canton else "$canton, $province") }
        }
        CITIES.forEach { (city, canton, province) -> add("$city, $canton, $province") }
    }.distinct()

    /**
     * Hasta [limit] lugares que contienen lo escrito, sin importar tildes ni mayúsculas. Primero
     * los que empiezan con eso ("que" → "Quepos" antes que "Ciudad Quesada").
     */
    fun search(query: String, limit: Int = 6): List<String> {
        val wanted = normalize(query)
        if (wanted.length < 2) return emptyList()
        return all
            .filter { normalize(it).contains(wanted) }
            .sortedBy { if (normalize(it).startsWith(wanted)) 0 else 1 }
            .take(limit)
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(MARKS, "").lowercase(Locale.ROOT).trim()

    private val MARKS = Regex("\\p{M}+")
}
