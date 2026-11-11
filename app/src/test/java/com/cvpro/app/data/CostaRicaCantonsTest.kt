package com.cvpro.app.data

import com.cvpro.app.data.location.CostaRicaCantons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cantón de las ciudades que se llaman distinto a su cantón (data/location/CostaRicaCantons.kt). */
class CostaRicaCantonsTest {

    @Test
    fun `Ciudad Quesada es de San Carlos aunque venga con tildes o Provincia de`() {
        assertEquals("San Carlos", CostaRicaCantons.cantonOf("Ciudad Quesada", "Provincia de Alajuela"))
        assertEquals("San Carlos", CostaRicaCantons.cantonOf("Quesada", "Alajuela"))
    }

    @Test
    fun `el mismo nombre en otra provincia es otro canton`() {
        assertEquals("Oreamuno", CostaRicaCantons.cantonOf("San Rafael", "Cartago"))
        assertEquals("Guatuso", CostaRicaCantons.cantonOf("San Rafael", "Alajuela"))
    }

    @Test
    fun `una ciudad que se llama como su canton no esta en la tabla`() {
        assertNull(CostaRicaCantons.cantonOf("Grecia", "Alajuela"))
    }
}
