package com.card.terminal

internal enum class CardCommunicationStep {
    CLEAR_UI,
    CARD_STATUS,
    ATR_CMD,
    UID_CMD,
    FW_CMD,
    READER_NAME_CMD,
    APDU_RESPONSE
}

internal data class CardCommunicationProgress @JvmOverloads constructor(
    val step: CardCommunicationStep, val message: String = "")