package com.cvpro.app.domain.model

/**
 * Los currículums del último editado al más viejo. Los que no traen fecha van al final.
 *
 * El servicio de CVs los devuelve sin orden, así que se ordenan acá, igual que la web
 * (`porUltimaEdicion` en cvService.ts). Vive en un solo lugar porque el panel principal y
 * "Mis currículums" muestran la misma lista: si cada pantalla ordenara a su manera, podrían
 * mostrar dos órdenes distintos de los mismos CVs.
 */
fun List<Cv>.byLastEdited(): List<Cv> = sortedByDescending { it.updatedAtMillis ?: Long.MIN_VALUE }
