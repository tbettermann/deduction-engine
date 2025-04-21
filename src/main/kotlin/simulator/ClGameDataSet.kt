package de.tb.simulator

import de.tb.evaluator.ClCard
import de.tb.evaluator.ClPlayer
import de.tb.simulator.exception.ClDataSetException
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

data class ClGameDataSet(
    val players: List<ClPlayer>,
    val allCards: Set<ClCard>,
    val solutionCards: Set<ClCard>,
    val leftOverCards: Set<ClCard>,
    val playerCards: Map<ClPlayer, Set<ClCard>> = emptyMap()
) {
    //region companion
    companion object {
        fun generateDefaultSet(
            clCards: Set<ClCard>,
            players: List<ClPlayer>,
            shuffled: Boolean = false
        ): ClGameDataSet {

            val cards = if (shuffled) clCards.shuffled() else clCards.sortedBy { it.id }

            val solutionSubject = cards.filterIsInstance<ClCard.SubjectCard>().shuffled().first()
            val solutionTool = cards.filterIsInstance<ClCard.ToolCard>().shuffled().first()
            val solutionRoom = cards.filterIsInstance<ClCard.RoomCard>().shuffled().first()
            val solutionCards = setOf(solutionSubject, solutionTool, solutionRoom)

            val leftOverCardCount = (cards.size - 3) % players.size
            val leftOverCards = cards
                .filterNot { clCard -> clCard.id in solutionCards.map { it.id } }
                .take(leftOverCardCount).toSet()

            val playerCardCount = (cards.size - 3 - leftOverCardCount) / players.size
            val playerCards: Map<ClPlayer, Set<ClCard>> = cards
                .filterNot { clCard ->
                    clCard.id in listOf(solutionCards, leftOverCards).flatten().map { it.id }
                }
                .chunked(playerCardCount)
                .mapIndexed { idx, cardChunk ->
                    ClPlayer(
                        position = idx,
                        isMe = idx == 0, // default first player is me
                        name = players[idx].name
                    ) to cardChunk.toSet()
                }.toMap()

            return ClGameDataSet(
                players = players,
                allCards = cards.toSet(),
                solutionCards = solutionCards,
                leftOverCards = leftOverCards,
                playerCards = playerCards
            )
        }
    }
    //endregion

    fun logDataSet() {
        val maxNameLength = players.maxOfOrNull { it.name.length } ?: 10
        logger.debug { "DataSet:" }
        logger.debug { "[SC]:  ${solutionCards.map { it.id }}" }
        logger.debug { "[LO]: ${leftOverCards.map { it.id }}" }
        players.forEach { pl -> logger.debug { "[P] ${pl.name.padEnd(maxNameLength)} -> ${playerCards[pl]?.map { it.id }}" } }
    }


    //region public
    fun getOwnCards(): Set<ClCard> =
        playerCards[getOwnPlayer()] ?: throw ClDataSetException("Own player cards not set.")
    //endregion

    //region private
    private fun getOwnPlayer(): ClPlayer =
        players.find { it.isMe } ?: throw ClDataSetException("Own player not set.")
    //endregion

}
