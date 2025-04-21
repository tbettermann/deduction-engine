package de.tb.evaluator

import de.tb.simulator.exception.ClGameSimulatorException


class ClGame(
    val name: String = "Default Game",
    val players: List<ClPlayer>,
    val allCards: Set<ClCard>,
    val leftOverCards: Set<ClCard>,
    val ownCards: Set<ClCard>,
    val turns: MutableList<ClTurn> = mutableListOf()
) {
    init {
        if (players.filter { it.isMe }.size != 1)
            throw ClGameSimulatorException("Exactly one player must be marked with isMe flag.")
    }

    fun addTurn(question: ClQuestion, answer: ClAnswer?) {
        turns.add(
            ClTurn(
                question = question,
                answer = answer,
                seqNr = turns.size
            )
        )
    }

    fun evaluate(): ClGameEvaluationResult =
        ClGameEvaluator
            .create(players = players, allCards = allCards, leftOverCards = leftOverCards, ownCards = ownCards)
            .updateMatrixFromTurns(turns = turns)
            .results()

    fun suggestedQuestionForMe(fixedRoomCard: ClCard.RoomCard? = null): ClQuestion? {
        TODO()
    }
}