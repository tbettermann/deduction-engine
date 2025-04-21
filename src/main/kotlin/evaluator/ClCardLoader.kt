package de.tb.evaluator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class ClCardLoader {
    companion object {
        fun loadFromFile(path: String): Set<ClCard> {
            val classLoader = Thread.currentThread().contextClassLoader
            val inputStream = classLoader.getResourceAsStream(path)
                ?: throw IllegalArgumentException("Resource not found: $path")

            val rawCards: Set<RawCard> = jacksonObjectMapper().readValue(inputStream)

            return rawCards.map { raw ->
                when (raw.type) {
                    CardType.ROOM -> ClCard.RoomCard(raw.id, raw.displayNames)
                    CardType.SUBJECT -> ClCard.SubjectCard(raw.id, raw.displayNames)
                    CardType.TOOL -> ClCard.ToolCard(raw.id, raw.displayNames)
                }
            }.toSet()
        }
    }

    data class RawCard(
        val type: CardType,
        val id: String,
        val displayNames: Map<String, String>
    )
}