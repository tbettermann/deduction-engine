package de.tb.simulator

import de.tb.evaluator.ClCard
import de.tb.evaluator.ClPlayer

data class ClPlayerEvaluationResult(
    val playerCards: Map<ClPlayer, Set<ClCard>>,
    val solutionCards: Set<ClCard>
)