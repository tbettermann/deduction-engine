package de.tb.evaluator

data class ClGameEvaluationResult(
    val exclusionMatrix: Map<Pair<ClPlayer, ClCard>, ClGameEvaluator.HasCard>,
    val solutionCards: Set<ClCard>
)