package com.cvpro.app.data.remote.dto

data class PagedResponseDto<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalRecords: Int,
)