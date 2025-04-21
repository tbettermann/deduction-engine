package de.tb.simulator

import de.tb.evaluator.*
import de.tb.evaluator.ClGameEvaluator.Companion.notClearCards
import de.tb.evaluator.ClGameEvaluator.Companion.playerCards
import de.tb.evaluator.ClPlayer.Companion.nextPlayerByPosition
import de.tb.evaluator.ClPlayer.Companion.sortByPosition
import de.tb.simulator.exception.ClGameSimulatorException
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

data class ClGameSimulator(
    val dataSet: ClGameDataSet
) {
    enum class QuestionStrategy { BASIC, EVALUATION_BASED }

    init {
        dataSet.logDataSet()
    }

    fun nextTurn(
        previousTurns: List<ClTurn>,
        strategy: QuestionStrategy = QuestionStrategy.BASIC,
        evaluationResult: ClGameEvaluationResult? = null
    ): Pair<ClQuestion, ClAnswer?> {

        // get active player based on turn
        val activePlayer = dataSet.players[(previousTurns.size) % dataSet.players.size]

        // generate question based on strategy
        val question =
            if (activePlayer.isMe && strategy == QuestionStrategy.EVALUATION_BASED) {
                evaluationResult?.let {
                    generateQuestionBasedOnEvaluationResult(
                        activePlayer = activePlayer,
                        evaluationResult = evaluationResult,
                        leftOverCards = dataSet.leftOverCards,
                        allCards = dataSet.allCards
                    )
                } ?: throw ClGameSimulatorException("Missing evaluation result for EVALUATION_BASED question strategy.")
            } else {
                val playerEvaluationResult = performBasicPlayerEvaluation(
                    player = activePlayer,
                    allPlayers = dataSet.players,
                    ownCards = dataSet.playerCards[activePlayer] ?: emptySet(),
                    leftOverCards = dataSet.leftOverCards,
                    turns = previousTurns
                )
                generateBasicQuestion(
                    activePlayer = activePlayer,
                    evaluationResult = playerEvaluationResult,
                    leftOverCards = dataSet.leftOverCards,
                    allCards = dataSet.allCards
                )
            }

        val answer = getAnswer(question = question)

        if (activePlayer.isMe) {
            logger.trace {
                "[Question](${previousTurns.size}): " +
                        "${question.player.name.padEnd(10)} -> ${question.cards.map { it.id }} "
            }
            logger.trace {
                "[Answer](${previousTurns.size})  : " +
                        "${answer?.player?.name?.padEnd(10)} -> ${answer?.cards?.map { it.id }} "
            }
        }

        return Pair(question, answer)
    }

    private fun performBasicPlayerEvaluation(
        player: ClPlayer,
        allPlayers: List<ClPlayer>,
        ownCards: Set<ClCard>,
        leftOverCards: Set<ClCard>,
        turns: List<ClTurn>
    ): ClPlayerEvaluationResult {

        // init solution vars
        val playerCards: Map<ClPlayer, MutableSet<ClCard>> = allPlayers.associateWith { mutableSetOf() }
        val solutionCards: MutableSet<ClCard> = mutableSetOf()

        // add players own cards
        playerCards[player]?.addAll(ownCards)

        // update player cards based on answers with exactly 1 card
        turns
            .filter { turn -> turn.answer?.cards?.size == 1 }
            .forEach { turn ->
                val pl = allPlayers.find { pl -> pl == turn.answer?.player }
                val card = turn.answer?.cards?.first()
                card?.let { playerCards[pl]?.add(it) }
            }

        // update solution cards based on answers with 0 cards
        turns
            .filter { turn -> turn.answer?.cards.isNullOrEmpty() }
            .forEach { turn ->
                playerCards[turn.question.player]?.let { askingPlayerCards ->
                    if (askingPlayerCards.size == 3) {
                        turn.question.cards
                            .filterNot { it in listOf(leftOverCards, askingPlayerCards).flatten() }.toSet()
                            .forEach {
                                solutionCards.add(it)
                            }
                    }
                }
            }

        return ClPlayerEvaluationResult(playerCards = playerCards, solutionCards = solutionCards)
    }

    private fun generateQuestionBasedOnEvaluationResult(
        activePlayer: ClPlayer,
        evaluationResult: ClGameEvaluationResult,
        leftOverCards: Set<ClCard>,
        allCards: Set<ClCard>
    ): ClQuestion {

        // priority list
        val priorityCards: List<ClCard> = listOf(
            notClearCards(exclusionMatrix = evaluationResult.exclusionMatrix).shuffled().toSet(),
            playerCards(exclusionMatrix = evaluationResult.exclusionMatrix, player = activePlayer).toSet(),
            evaluationResult.solutionCards,
            leftOverCards,
            ClGameEvaluator.otherPlayerCardsInReverseOrder(
                exclusionMatrix = evaluationResult.exclusionMatrix,
                players = dataSet.players,
                excludedPlayer = activePlayer
            ),
            allCards
        ).onEachIndexed { idx, s ->
            if (activePlayer.isMe) logger.trace { "[E$idx](${s.size}): ${s.map { it.id }}" }
        }.flatten()

        return ClQuestion(
            player = activePlayer,
            cards = setOf(
                priorityCards.filterIsInstance<ClCard.SubjectCard>().first(),
                priorityCards.filterIsInstance<ClCard.ToolCard>().first(),
                priorityCards.filterIsInstance<ClCard.RoomCard>().first()
            )
        )

    }

    private fun generateBasicQuestion(
        activePlayer: ClPlayer,
        evaluationResult: ClPlayerEvaluationResult,
        leftOverCards: Set<ClCard>,
        allCards: Set<ClCard>
    ): ClQuestion {

        // all not known cards
        val openCards = dataSet.allCards
            .filterNot {
                it in setOf(
                    leftOverCards,
                    evaluationResult.solutionCards,
                    evaluationResult.playerCards[activePlayer] ?: emptyList(),
                    evaluationResult.playerCards.filterKeys { pl -> pl != activePlayer }.values.flatten()
                ).flatten()
            }.toSet()

        // ranked list of candidate cards for selection
        val priorityCards: List<ClCard> =
            listOf(
                openCards.shuffled().toSet(),
                evaluationResult.playerCards[activePlayer] ?: emptySet(),
                evaluationResult.solutionCards,
                leftOverCards,
                allCards
            ).onEachIndexed { idx, s ->
                if (activePlayer.isMe) logger.trace { "[B$idx](${s.size}): ${s.map { it.id }}" }
            }.flatten()

        return ClQuestion(
            player = activePlayer,
            cards = setOf(
                priorityCards.filterIsInstance<ClCard.SubjectCard>().first(),
                priorityCards.filterIsInstance<ClCard.ToolCard>().first(),
                priorityCards.filterIsInstance<ClCard.RoomCard>().first()
            )
        )
    }

    private fun getAnswer(question: ClQuestion): ClAnswer? {

        // get first card which matches based on player positions
        val answer = dataSet.players
            .sortByPosition(dataSet.players.nextPlayerByPosition(question.player))
            .firstNotNullOfOrNull { player ->
                if (question.player.name != player.name) {
                    question.cards
                        .find { card -> card in (dataSet.playerCards[player] ?: emptySet()) }
                        ?.let { Pair(player, it) }
                } else null
            }

        return answer?.let {
            ClAnswer(
                player = answer.first,
                cards = if (question.player.isMe) setOf(answer.second) else question.cards
            )
        }
    }

}