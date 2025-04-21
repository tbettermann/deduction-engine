package de.tb.evaluator

data class ClTurn(
    val question: ClQuestion,
    val answer: ClAnswer?,
    val seqNr: Int = 0
)