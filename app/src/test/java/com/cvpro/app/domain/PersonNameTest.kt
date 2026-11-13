package com.cvpro.app.domain

import com.cvpro.app.domain.model.personName
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonNameTest {

    @Test
    fun `un correo nunca se usa como nombre`() {
        assertEquals("", personName("ana@gmail.com"))
        assertEquals("", personName(null))
        assertEquals("Ana Mora", personName("  Ana Mora "))
    }
}
