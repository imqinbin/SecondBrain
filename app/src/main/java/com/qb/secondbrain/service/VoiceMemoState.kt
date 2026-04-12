package com.qb.secondbrain.service

sealed class VoiceMemoState {
    data object Idle : VoiceMemoState()
    data object Recording : VoiceMemoState()
    data class Processing(val step: String = "") : VoiceMemoState()
    data class Notifying(val message: String) : VoiceMemoState()
    data class Error(val message: String) : VoiceMemoState()
}
