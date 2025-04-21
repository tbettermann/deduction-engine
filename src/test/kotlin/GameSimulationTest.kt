package de.tb.test

import de.tb.evaluator.ClCardLoader
import de.tb.evaluator.ClGame
import de.tb.evaluator.ClGameEvaluator.Companion.logExclusionMatrix
import de.tb.evaluator.ClPlayer
import de.tb.simulator.ClGameDataSet
import de.tb.simulator.ClGameSimulator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private val logger = KotlinLogging.logger {}

class GameSimulationTest {

    companion object {
        const val MAX_ROUNDS = 150
        const val MAX_GAMES = 1000

        fun logTurnStats(turns: List<Int>) =
            logger.info {
                "Executed $MAX_GAMES games with " +
                        "AVG(${turns.average()}) / " +
                        "MED(${turns.sortedBy { it }[turns.size / 2]}) / " +
                        "MIN(${turns.min()}) / " +
                        "MAX(${turns.max()}). "
            }
    }

    @Test
    fun singleSimulationTest() {

        val cards = ClCardLoader.loadFromFile(path = "cards.json")

        val players: List<ClPlayer> = listOf(
            ClPlayer(0, "Anna", isMe = true),
            ClPlayer(1, "Ben"),
            ClPlayer(2, "Chris"),
            ClPlayer(3, "Daniel"),
            ClPlayer(4, "Emil")
        )

        val turns: MutableList<Int> = mutableListOf()
        for (g in (0..<MAX_GAMES)) {

            // create dataset
            val dataSet = ClGameDataSet.generateDefaultSet(clCards = cards, players = players, shuffled = true)

            // create game simulator
            val simulator = ClGameSimulator(dataSet = dataSet)

            // create a game
            val game = ClGame(
                name = "Standard Game 1",
                allCards = cards,
                players = players,
                leftOverCards = dataSet.leftOverCards,
                ownCards = dataSet.getOwnCards()
            )

            for (i in (0..<MAX_ROUNDS)) {

                val result = game.evaluate()
                if (result.solutionCards.size == 3) {
                    // make sure correct solution is found
                    assertEquals(dataSet.solutionCards, result.solutionCards)
                    turns.add(i)
                    break
                }

                val (question, answer) = simulator.nextTurn(
                    previousTurns = game.turns,
                    strategy = ClGameSimulator.QuestionStrategy.EVALUATION_BASED,
                    evaluationResult = result
                )
                game.addTurn(question = question, answer = answer)
            }
        }
        logTurnStats(turns = turns)
    }

    @Test
    fun multiSimulationTest() {

        val cards = ClCardLoader.loadFromFile(path = "cards.json")

        val players: List<ClPlayer> = listOf(
            ClPlayer(0, "Anna", isMe = true),
            ClPlayer(1, "Ben"),
            ClPlayer(2, "Chris"),
            ClPlayer(3, "Daniel"),
            ClPlayer(4, "Emil")
        )

        val turns1: MutableList<Int> = mutableListOf()
        val turns2: MutableList<Int> = mutableListOf()

        var cancel1 = false
        var cancel2 = false

        for (g in (0..<MAX_GAMES)) {

            // create dataset
            val dataSet = ClGameDataSet.generateDefaultSet(clCards = cards, players = players, shuffled = true)

            // create game simulator
            val simulator1 = ClGameSimulator(dataSet = dataSet)
            val simulator2 = ClGameSimulator(dataSet = dataSet)

            // create a game
            val game1 = ClGame(
                name = "Standard Game 1",
                allCards = cards,
                players = players,
                leftOverCards = dataSet.leftOverCards,
                ownCards = dataSet.getOwnCards()
            )
            val game2 = ClGame(
                name = "Standard Game 2",
                allCards = cards,
                players = players,
                leftOverCards = dataSet.leftOverCards,
                ownCards = dataSet.getOwnCards()
            )

            for (i in (0..<MAX_ROUNDS)) {

                if (!cancel1) {
                    val result = game1.evaluate()
                    if (result.solutionCards.size == 3) {
                        // make sure right solution is found
                        assertEquals(dataSet.solutionCards, result.solutionCards)

                        logger.debug { "[G1] Solution found in turn($i): ${result.solutionCards.map { it.id }}" }
                        logExclusionMatrix(exclusionMatrix = result.exclusionMatrix, onlyStats = true)

                        turns1.add(i)
                        cancel1 = true
                    }
                    val (question1, answer1) = simulator1.nextTurn(
                        previousTurns = game1.turns,
                        strategy = ClGameSimulator.QuestionStrategy.EVALUATION_BASED,
                        evaluationResult = result
                    )
                    game1.addTurn(question = question1, answer = answer1)
                }

                if (!cancel2) {
                    val result = game2.evaluate()
                    if (result.solutionCards.size == 3) {
                        // make sure correct solution is found
                        assertEquals(dataSet.solutionCards, result.solutionCards)

                        logger.debug { "[G2] Solution found in turn($i): ${result.solutionCards.map { it.id }}" }
                        logExclusionMatrix(exclusionMatrix = result.exclusionMatrix, onlyStats = true)

                        turns2.add(i)
                        cancel2 = true
                    }
                    val (question2, answer2) = simulator2.nextTurn(
                        previousTurns = game2.turns,
                        strategy = ClGameSimulator.QuestionStrategy.BASIC,
                    )
                    game2.addTurn(question = question2, answer = answer2)
                }

                if (cancel1 && cancel2) {
                    cancel1 = false
                    cancel2 = false
                    break
                }
            }
        }
        logTurnStats(turns = turns1)
        logTurnStats(turns = turns2)
    }
}