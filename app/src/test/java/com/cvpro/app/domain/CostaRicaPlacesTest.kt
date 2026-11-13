package com.cvpro.app.domain

import com.cvpro.app.domain.model.CostaRicaPlaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CostaRicaPlacesTest {

    @Test
    fun `los cantones llevan provincia y las ciudades su canton`() {
        assertTrue(CostaRicaPlaces.all.contains("San Carlos, Alajuela"))
        assertTrue(CostaRicaPlaces.all.contains("Ciudad Quesada, San Carlos, Alajuela"))
    }

    @Test
    fun `busca sin tildes ni mayusculas y primero lo que empieza igual`() {
        assertEquals("Quepos, Puntarenas", CostaRicaPlaces.search("quep").first())
        assertTrue(CostaRicaPlaces.search("ques").contains("Ciudad Quesada, San Carlos, Alajuela"))
        assertTrue(CostaRicaPlaces.search("limon").contains("Limón"))
    }

    @Test
    fun `con menos de dos letras no sugiere nada`() {
        assertTrue(CostaRicaPlaces.search("q").isEmpty())
    }
}
