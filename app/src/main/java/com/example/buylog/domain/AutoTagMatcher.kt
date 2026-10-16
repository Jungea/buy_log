package com.example.buylog.domain

data class AutoTagPattern(val phrase: String, val tagId: Long)

object AutoTagMatcher {
    fun matchingTagIds(
        merchant: String,
        memo: String,
        patterns: List<AutoTagPattern>,
    ): Set<Long> {
        val searchableText = "$merchant\n$memo"
        return patterns
            .filter { it.phrase.isNotBlank() && searchableText.contains(it.phrase.trim(), ignoreCase = true) }
            .mapTo(mutableSetOf()) { it.tagId }
    }
}
