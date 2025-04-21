package de.tb.test.unit

import de.tb.evaluator.ClCard
import de.tb.evaluator.ClCardLoader
import de.tb.evaluator.ClGameEvaluator
import de.tb.evaluator.ClGameEvaluator.Companion.completeExcludedCards
import de.tb.evaluator.ClGameEvaluator.Companion.playerCards
import de.tb.evaluator.ClPlayer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClGameEvaluatorTest {

    private val allCards: Set<ClCard> = ClCardLoader.loadFromFile(path = "cards_small.json")
    private val players: List<ClPlayer> = listOf(
        ClPlayer(0, "Anna", isMe = true),
        ClPlayer(1, "Ben"),
        ClPlayer(2, "Chris"),
        ClPlayer(3, "Daniel")
    )

    @Test
    fun `should initialize correctly`() {

        val leftOverCards = allCards.take(2).toSet()
        val ownCards = (allCards - leftOverCards).take(3).toSet()

        val evaluator = ClGameEvaluator.create(
            players = players,
            allCards = allCards,
            leftOverCards = leftOverCards,
            ownCards = ownCards
        )


        val matrix = evaluator.results().exclusionMatrix
        val solution = evaluator.results().solutionCards

        // own player cards correctly included
        assertEquals(
            ownCards.sortedBy { it.id },
            playerCards(exclusionMatrix = matrix, player = players.find { it.isMe }!!).sortedBy { it.id }
        )

        // left over cards correctly excluded
        assertEquals(
            leftOverCards.sortedBy { it.id },
            completeExcludedCards(exclusionMatrix = matrix).sortedBy { it.id }
        )

        // solution is empty
        assertEquals(emptySet(), solution)
    }
}