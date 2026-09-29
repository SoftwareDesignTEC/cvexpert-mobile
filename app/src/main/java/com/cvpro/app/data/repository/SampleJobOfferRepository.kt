package com.cvpro.app.data.repository

import com.cvpro.app.domain.model.JobModality
import com.cvpro.app.domain.model.JobOffer
import com.cvpro.app.domain.model.PagedResult
import com.cvpro.app.domain.repository.JobOfferRepository
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ofertas de prueba mientras no existan los mocks del APIM.
 *
 * Imita lo que hará el servicio: espera un momento, filtra por el texto buscado y devuelve
 * de a una página con totalRecords, para poder armar y probar la paginación desde ahora.
 *
 * TODO: reemplazar por la implementación con Retrofit contra el APIM. Esta clase se borra.
 */
@Singleton
class SampleJobOfferRepository @Inject constructor() : JobOfferRepository {

    override suspend fun getOffers(page: Int, pageSize: Int, query: String): PagedResult<JobOffer> {
        // Simula el tiempo de red, para que se vea el estado de carga.
        delay(NETWORK_DELAY_MS)

        // Mientras no haya servicio, esta palabra sirve para ver el estado de error.
        // TODO: quitar cuando el APIM devuelva errores de verdad.
        if (query.trim().equals(ERROR_KEYWORD, ignoreCase = true)) {
            throw IOException("Búsqueda de prueba que falla a propósito")
        }

        val filtered = if (query.isBlank()) {
            SAMPLE_OFFERS
        } else {
            SAMPLE_OFFERS.filter { offer ->
                val texto = "${offer.title} ${offer.company} ${offer.sector} ${offer.location}"
                texto.contains(query.trim(), ignoreCase = true)
            }
        }

        val fromIndex = (page - 1) * pageSize
        val items = filtered.drop(fromIndex).take(pageSize)

        return PagedResult(
            items = items,
            page = page,
            pageSize = pageSize,
            totalRecords = filtered.size,
        )
    }

    private companion object {
        const val NETWORK_DELAY_MS = 700L
        const val ERROR_KEYWORD = "error"

        val SAMPLE_OFFERS: List<JobOffer> = listOf(
            offer("1", "Asistente administrativo", "Grupo Monge", "San José", JobModality.ON_SITE, "Administración", 82, "Bachillerato en secundaria", "Excel básico"),
            offer("2", "Cajero de tienda", "Automercado", "Heredia", JobModality.ON_SITE, "Comercio", 76, "Atención al cliente", "Disponibilidad de fines de semana"),
            offer("3", "Agente de servicio al cliente", "Teleperformance", "Heredia", JobModality.HYBRID, "Servicio al cliente", 71, "Inglés B2", "Manejo de computadora"),
            offer("4", "Practicante de mercadeo digital", "Pozuelo", "San José", JobModality.HYBRID, "Mercadeo", 68, "Estudiante de mercadeo", "Manejo de redes sociales"),
            offer("5", "Soporte técnico nivel 1", "Grupo ICE", "Cartago", JobModality.ON_SITE, "Tecnología", 64, "Conocimientos de redes", "Disponibilidad de horario"),
            offer("6", "Asistente de bodega", "Walmart", "Alajuela", JobModality.ON_SITE, "Logística", 61, "Trabajo en equipo", "Licencia B1 deseable"),
            offer("7", "Community manager junior", "Agencia Nova", "Todo el país", JobModality.REMOTE, "Mercadeo", 58, "Redacción", "Portafolio de contenidos"),
            offer("8", "Recepcionista bilingüe", "Hotel Presidente", "San José", JobModality.ON_SITE, "Turismo", 55, "Inglés conversacional", "Presentación personal"),
            offer("9", "Desarrollador web junior", "Gorilla Logic", "Todo el país", JobModality.REMOTE, "Tecnología", 52, "HTML, CSS y JavaScript", "Portafolio o proyectos propios"),
            offer("10", "Auxiliar contable", "Despacho Carvajal", "Heredia", JobModality.HYBRID, "Finanzas", 49, "Estudiante de contaduría", "Excel intermedio"),
            offer("11", "Promotor de ventas", "Distribuidora La Florida", "Puntarenas", JobModality.ON_SITE, "Ventas", 45, "Facilidad de palabra", "Disponibilidad para viajar"),
            offer("12", "Asistente de cocina", "Restaurante Silvestre", "San José", JobModality.ON_SITE, "Alimentos", 42, "Manipulación de alimentos", "Trabajo bajo presión"),
        )

        fun offer(
            id: String,
            title: String,
            company: String,
            location: String,
            modality: JobModality,
            sector: String,
            matchScore: Int,
            vararg requirements: String,
        ) = JobOffer(
            id = id,
            title = title,
            company = company,
            // Los datos de prueba no traen logo: la tarjeta cae al recuadro con la inicial.
            companyLogoUrl = null,
            location = location,
            modality = modality,
            sector = sector,
            requirements = requirements.toList(),
            matchScore = matchScore,
        )
    }
}
