package de.tb.evaluator

import java.util.*

enum class CardType { ROOM, SUBJECT, TOOL }

sealed class ClCard {
    abstract val id: String
    abstract val displayNames: Map<String, String>

    fun getDisplayName(locale: Locale = Locale.getDefault()): String {
        return displayNames[locale.language] ?: displayNames["en"] ?: id
    }

    fun cardType(): CardType = when (this) {
        is RoomCard -> CardType.ROOM
        is SubjectCard -> CardType.SUBJECT
        is ToolCard -> CardType.TOOL
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClCard

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    data class ToolCard(override val id: String, override val displayNames: Map<String, String>) : ClCard()
    data class RoomCard(override val id: String, override val displayNames: Map<String, String>) : ClCard()
    data class SubjectCard(override val id: String, override val displayNames: Map<String, String>) : ClCard()

}