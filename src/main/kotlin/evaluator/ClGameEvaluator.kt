package de.tb.evaluator

import de.tb.evaluator.ClPlayer.Companion.inBetween
import de.tb.evaluator.ClPlayer.Companion.nextPlayerByPosition
import de.tb.evaluator.ClPlayer.Companion.sortByPosition
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class ClGameEvaluator(
    private val exclusionMatrix: MutableMap<Pair<ClPlayer, ClCard>, HasCard>,
    private val solutionCards: MutableSet<ClCard> = mutableSetOf(),
    private val allCards: Set<ClCard>,
    private val leftOverCards: Set<ClCard>,
    private val players: List<ClPlayer>,
    private val maxPlayerCards: Int
) {

    enum class HasCard { YES, NO, NOT_CLEAR }

    //region companion
    companion object {

        fun create(
            players: List<ClPlayer>,
            allCards: Set<ClCard>,
            leftOverCards: Set<ClCard>,
            ownCards: Set<ClCard>
        ): ClGameEvaluator {

            // (0) init matrix
            val exclusionMatrix = players
                .map { pl -> allCards.map { card -> Pair(pl, card) } }.flatten()
                .associateWith { _ -> HasCard.NOT_CLEAR }
                .toMutableMap()

            // add own cards, left-overs cards
            players.forEach { pl ->
                leftOverCards.forEach { card -> exclusionMatrix[pl to card] = HasCard.NO }
                if (pl.isMe) ownCards.forEach { card -> exclusionMatrix[pl to card] = HasCard.YES }
                else ownCards.forEach { card -> exclusionMatrix[pl to card] = HasCard.NO }
            }

            return ClGameEvaluator(
                exclusionMatrix = exclusionMatrix,
                allCards = allCards,
                leftOverCards = leftOverCards,
                players = players,
                maxPlayerCards = ownCards.size
            )
        }

        fun playerCards(exclusionMatrix: Map<Pair<ClPlayer, ClCard>, HasCard>, player: ClPlayer): Set<ClCard> =
            exclusionMatrix
                .filter { (k, v) -> (k.first == player) && (v == HasCard.YES) }
                .map { (k, _) -> k.second }.toSet()

        fun playerCardSet(exclusionMatrix: Map<Pair<ClPlayer, ClCard>, HasCard>): Set<ClCard> =
            exclusionMatrix
                .filter { (_, v) -> v == HasCard.YES }
                .map { (k, _) -> k.second }.toSet()

        fun playerCardMap(
            exclusionMatrix: Map<Pair<ClPlayer, ClCard>, HasCard>,
            players: List<ClPlayer>
        ): Map<ClPlayer, Set<ClCard>> {
            return players.associateWith { pl -> playerCards(exclusionMatrix = exclusionMatrix, player = pl) }
        }

        fun otherPlayerCardsInReverseOrder(
            exclusionMatrix: Map<Pair<ClPlayer, ClCard>, HasCard>,
            players: List<ClPlayer>,
            excludedPlayer: ClPlayer
        ): List<ClCard> = players
            .filterNot { it == excludedPlayer }
            .sortByPosition(players.nextPlayerByPosition(player = excludedPlayer))
            .reversed()
            .map { pl -> playerCards(exclusionMatrix = exclusionMatrix, player = pl) }.flatten()

        fun nonPlayerCards(
            exclusionMatrix: Map<Pair<ClPlayer, ClCard>, HasCard>,
            player: ClPlayer
        ): Set<ClCard> = exclusionMatrix
            .filter { (k, v) -> (k.first == player) && (v == HasCard.NO) }
            .map { (k, _) -> k.second }.toSet()

        fun notClearCards(exclusionMatrix: Map<Pair<ClPlayer, ClCard>, HasCard>): Set<ClCard> =
            exclusionMatrix
                .filter { (_, v) -> v == HasCard.NOT_CLEAR }
                .map { (k, _) -> k.second }.toSet()

        fun completeExcludedCards(
            exclusionMatrix: Map<Pair<ClPlayer, ClCard>, HasCard>
        ): Set<ClCard> {
            val cards = exclusionMatrix.keys.map { it.second }.toSet()
            return cards
                .filter { it ->
                    exclusionMatrix.filter { (k, _) -> (k.second == it) }
                        .values.all { it == HasCard.NO }
                }.toSet()
        }

        fun logExclusionMatrix(
            exclusionMatrix: Map<Pair<ClPlayer, ClCard>, HasCard>,
            onlyStats: Boolean = false,
            caption: String = ""
        ) {
            val players = exclusionMatrix.keys.map { it.first }.distinct()
            val cards = exclusionMatrix.keys.map { it.second }.distinct().sortedBy { it.cardType().ordinal }

            val cardColWidth = cards.maxOf { it.id.length }.coerceAtLeast(caption.length) + 2
            val playerColWidth = players.maxOf { it.name.length } + 2

            logger.trace {
                buildString {

                    appendLine()
                    // Header row
                    appendLine(caption.padEnd(cardColWidth) + players.joinToString("") { it.name.padEnd(playerColWidth) })

                    // Divider line
                    appendLine("".padEnd(cardColWidth + playerColWidth * players.size, '='))
                    if (!onlyStats) {
                        // Matrix rows
                        for (card in cards) {
                            append(card.id.padEnd(cardColWidth))
                            for (player in players) {
                                val value = exclusionMatrix[Pair(player, card)] ?: HasCard.NOT_CLEAR
                                val symbol = when (value) {
                                    HasCard.YES -> "o"
                                    HasCard.NO -> "-"
                                    HasCard.NOT_CLEAR -> ""
                                }
                                append(symbol.padEnd(playerColWidth))
                            }
                            appendLine()
                        }
                    }

                    // footer
                    val playerCardCount = playerCardSet(exclusionMatrix).size
                    val yesCounts =
                        players.map { player -> cards.count { exclusionMatrix[Pair(player, it)] == HasCard.YES } }
                    val noCounts =
                        players.map { player -> cards.count { exclusionMatrix[Pair(player, it)] == HasCard.NO } }
                    val unknownCounts = players.map { player ->
                        cards.count { (exclusionMatrix[Pair(player, it)] ?: HasCard.NOT_CLEAR) == HasCard.NOT_CLEAR }
                    }

                    append("PLC($playerCardCount)".padEnd(cardColWidth))
                    yesCounts.forEach { append("✔$it".padEnd(playerColWidth)) }
                    appendLine()

                    append("".padEnd(cardColWidth))
                    noCounts.forEach { append("✖$it".padEnd(playerColWidth)) }
                    appendLine()

                    append("".padEnd(cardColWidth))
                    unknownCounts.forEach { append("?$it".padEnd(playerColWidth)) }
                    appendLine()
                }
            }
        }

    }

    //endregion

    //region public
    /**
     * Updates the deduction matrix based on the provided list of turns.
     *
     * The process follows several logical steps to infer card ownership and possible solutions:
     *
     * ### Step 0: Initialize Matrix
     * - Add own cards and leftover (unassigned) cards.
     *
     * ### Step 1: Process Turns
     * - Exclude question cards that were not shown as answers.
     * - If exactly one answer is given, assign the corresponding card to the answering player.
     *
     * ### Step 2: Deduce Solutions
     * - Identify possible solution cards from turns with no answer.
     * - If all players are excluded for a specific card (and it's not leftover), mark it as a solution.
     * - If all but one card in a group are known, the remaining one must be the solution.
     *
     * ### Step 3: Apply Implicit Rules
     * - If all cards for a player are known, exclude all other possibilities for that player.
     * - If all non-player cards are identified, the remaining cards must belong to players.
     *
     * ### Step 4: Refine Turns Based on Matrix
     * - Eliminate impossible answers from each turn based on current matrix state.
     *
     * If any updates are made during the process, the evaluation restarts from Step 1.
     *
     * @param turns A list of [ClTurn] representing player turns with questions and responses.
     * @return The updated [ClGameEvaluator] instance reflecting the new matrix state.
     */
    fun updateMatrixFromTurns(turns: List<ClTurn>): ClGameEvaluator {

        val notClearCardsBefore = notClearCards(exclusionMatrix).size

        // (1) update from turns (matrix updates)
        // update matrix for players in between, which are not showing a card
        turns.forEach { turn ->
            turn.answer?.let { answer ->
                players
                    .sortByPosition(turn.question.player)
                    .inBetween(turn.question.player, answer.player)
                    .forEach { pl ->
                        turn.question.cards.forEach { card ->
                            exclusionMatrix[pl to card] = HasCard.NO
                        }
                    }
            }
        }

        // add cards from answers
        turns
            .filter { turn -> turn.answer?.cards?.size == 1 }
            .forEach { turn ->
                turn.answer?.let { answer ->
                    val card = answer.cards.first()
                    players.forEach { pl -> exclusionMatrix[pl to card] = HasCard.NO }
                    exclusionMatrix[answer.player to card] = HasCard.YES
                }
            }

        // (2) find solution cards (solution card updates, matrix updates)
        // (a) solution card could be found from turns with empty answer
        turns
            .filter { turn -> turn.answer?.cards.isNullOrEmpty() }
            .forEach { turn ->
                playerCards(turn.question.player).let { askingPlayerCards ->
                    if (askingPlayerCards.size == maxPlayerCards) {
                        turn.question.cards
                            .filterNot { it in listOf(leftOverCards, askingPlayerCards).flatten() }
                            .toSet()
                            .forEach { card ->
                                solutionCards.add(card)
                                players.forEach { pl -> exclusionMatrix[pl to card] = HasCard.NO }
                            }
                    }
                }
            }

        // (b) if for a card all player excluded (and not left-over) -> solution
        (completeExcludedCards() - leftOverCards).forEach { cards -> solutionCards.add(cards) }

        // (c) if all cards except one found in card group -> solution
        (allCards - leftOverCards - playerCardSet())
            .groupBy { it.javaClass.kotlin }
            .filter { (_, v) -> v.size == 1 }
            .forEach { (_, v) ->
                solutionCards.add(v.first())
                players.forEach { pl -> exclusionMatrix[pl to v.first()] = HasCard.NO }
            }

        // (3) apply implicit rules (matrix updates)
        // (a) found all cards for a player -> exclude the others
        players.forEach { pl ->
            val playerCards = playerCards(pl)
            if (playerCards.size == maxPlayerCards) {
                (allCards - playerCards).forEach { card -> exclusionMatrix[pl to card] = HasCard.NO }
            }
        }

        // (b) add all player cards if allCards.size - nonPlayerCards.size = maxPlayerCards
        players.forEach { pl ->
            val nonPlayerCards = nonPlayerCards(pl)
            if ((allCards.size - nonPlayerCards.size) == maxPlayerCards) {
                (allCards - nonPlayerCards).forEach { card -> exclusionMatrix[pl to card] = HasCard.YES }
            }
        }


        // (4) update turns from matrix
        val updatedTurns = turns.map { turn ->
            // remove cards from answers, which the answering player certainly don't have
            val updatedAnswer = turn.answer?.let { answer ->
                val nonPlayerCards = nonPlayerCards(answer.player)
                ClAnswer(
                    player = answer.player,
                    cards = (answer.cards - nonPlayerCards)
                )
            }
            ClTurn(
                question = turn.question,
                answer = updatedAnswer,
                seqNr = turn.seqNr
            )
        }

        // turn information has changed?-> call method again
        return if (updatedTurns != turns || notClearCards(exclusionMatrix).size != notClearCardsBefore)
            updateMatrixFromTurns(turns = updatedTurns)
        else this
    }

    fun results(): ClGameEvaluationResult {
        return ClGameEvaluationResult(
            exclusionMatrix = exclusionMatrix,
            solutionCards = solutionCards
        )
    }
    //endregion

    //region private
    private fun playerCards(player: ClPlayer) =
        Companion.playerCards(exclusionMatrix = exclusionMatrix, player = player)

    private fun playerCardSet() =
        Companion.playerCardSet(exclusionMatrix = exclusionMatrix)

    private fun nonPlayerCards(player: ClPlayer) =
        Companion.nonPlayerCards(exclusionMatrix = exclusionMatrix, player = player)

    private fun completeExcludedCards() =
        Companion.completeExcludedCards(exclusionMatrix = exclusionMatrix)
    //endregion
}