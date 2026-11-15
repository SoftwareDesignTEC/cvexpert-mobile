package com.cvpro.app.domain.model

/**
 * Lo que el usuario dijo que busca en su perfil: jornadas ("full-time"...) y modalidades.
 *
 * Es el mismo orden que usa la web (jobRanking.ts de Pérez): si cada app ordenara por su cuenta,
 * mostrarían ofertas distintas diciendo "Recomendadas".
 */
data class JobPreferences(
    val types: Set<String> = emptySet(),
    val modalities: Set<JobModality> = emptySet(),
) {
    /** Si eligió al menos una preferencia. Sin ninguna se ordena solo por compatibilidad. */
    val isEmpty: Boolean get() = types.isEmpty() && modalities.isEmpty()

    /**
     * Cuántas preferencias cumple la oferta: 0, 1 o 2.
     *
     * Una lista vacía no suma ni resta: quien no eligió modalidad no está diciendo que le da
     * igual, está diciendo que no lo especificó.
     */
    fun affinity(offer: JobOffer): Int {
        var points = 0
        if (modalities.isNotEmpty() && offer.modality in modalities) points++
        if (types.isNotEmpty() && types.any { it.equals(offer.type, ignoreCase = true) }) points++
        return points
    }

    companion object {
        val NONE = JobPreferences()

        fun of(profile: UserProfile): JobPreferences =
            JobPreferences(types = profile.types.toSet(), modalities = profile.modalities.toSet())
    }
}

/**
 * Primero las ofertas que cumplen más preferencias y, a igualdad, las de mayor compatibilidad.
 *
 * Las preferencias mandan sobre el puntaje porque son una elección explícita: si pidió remoto,
 * una presencial con 90 % de compatibilidad no es lo que quiere ver primero. El orden es estable:
 * dos ofertas iguales en todo quedan como vinieron del servicio.
 */
fun List<JobOffer>.rankedFor(preferences: JobPreferences): List<JobOffer> =
    sortedWith(
        compareByDescending<JobOffer> { preferences.affinity(it) }
            .thenByDescending { it.matchScore ?: 0 },
    )
