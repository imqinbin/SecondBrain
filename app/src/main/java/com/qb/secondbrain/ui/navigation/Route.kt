package com.qb.secondbrain.ui.navigation

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable
    data object MemoList : Route()

    @Serializable
    data class MemoDetail(val id: Long) : Route()

    @Serializable
    data class MemoEdit(val id: Long = -1L) : Route()

    @Serializable
    data class Search(val query: String = "") : Route()

    @Serializable
    data object Settings : Route()
}
