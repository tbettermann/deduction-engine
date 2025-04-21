package de.tb

import de.tb.evaluator.ClCardLoader
import de.tb.evaluator.ClGame
import de.tb.evaluator.ClPlayer
import de.tb.simulator.ClGameDataSet
import de.tb.simulator.ClGameSimulator
import io.github.oshai.kotlinlogging.KotlinLogging

const val MAX_ROUNDS = 100

private val logger = KotlinLogging.logger {}

fun main() {

    // load cards
    val cards = ClCardLoader.loadFromFile(path = "i18n/cards.json")

    // create player
    val players: List<ClPlayer> = listOf(
        ClPlayer(0, "Anna", isMe = true),
        ClPlayer(1, "Ben"),
        ClPlayer(2, "Chris"),
        ClPlayer(3, "Daniel"),
        ClPlayer(4, "Emil")
    )

    // create dataset (only for simulation)
    val dataSet = ClGameDataSet.generateDefaultSet(clCards = cards, players = players, shuffled = true)
    // create game simulator (only for simulation)
    val simulator = ClGameSimulator(dataSet = dataSet)

    // create a game
    val game = ClGame(
        name = "Standard Game",
        allCards = cards,
        players = players,
        leftOverCards = dataSet.leftOverCards,
        ownCards = dataSet.getOwnCards()
    )

    // game loop
    for (i in (0..<MAX_ROUNDS)) {

        val result = game.evaluate()
        if (result.solutionCards.size == 3) {
            logger.info { "Solution found in turn($i): ${result.solutionCards.map { it.id }}" }
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