package de.tb.evaluator

data class ClPlayer(
    val position: Int,
    val name: String,
    val isMe: Boolean = false

) {
    //region companion
    companion object {
        fun List<ClPlayer>.nextPlayerByPosition(player: ClPlayer): ClPlayer? {
            val sorted = this.sortByPosition()
            if (sorted.isEmpty()) return null
            if (sorted.size == 1) return sorted.first()
            val idx = sorted.indexOf(player)
            if (idx == -1) return null
            return if (idx == size - 1) sorted.first() else sorted[idx + 1]
        }

        fun List<ClPlayer>.sortByPosition(first: ClPlayer? = null): List<ClPlayer> {
            return if (first == null) this.sortedBy { it.position }
            else this.sortedWith(compareBy<ClPlayer> { it.position < first.position }.thenBy { it.position })
        }

        fun List<ClPlayer>.inBetween(first: ClPlayer, second: ClPlayer): List<ClPlayer> {
            if (this.indexOf(first) == -1 || this.indexOf(second) == -1) return emptyList()
            return this.slice(this.indexOf(first) + 1..<this.indexOf(second))
        }
    }
    //endregion
}