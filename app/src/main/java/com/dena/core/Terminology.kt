package com.dena.core

object Terminology {
    data class Labels(
        val tabBorrowed: String,
        val tabLent: String,
        val directionOwedToMe: String,
        val directionIOwe: String,
        val screenIOweTitle: String,
        val bannerYouOwe: String,
        val bannerOwedToYou: String,
    )

    fun labels(mode: String): Labels = if (mode == DenaPreferences.TERM_OWED) Labels(
        tabBorrowed = "I Owe",
        tabLent = "Owed to Me",
        directionOwedToMe = "Owed to Me",
        directionIOwe = "I Owe",
        screenIOweTitle = "I Owe",
        bannerYouOwe = "you owe",
        bannerOwedToYou = "owed to you",
    ) else Labels(
        tabBorrowed = "I Borrowed",
        tabLent = "I Lent",
        directionOwedToMe = "I Lent",
        directionIOwe = "I Borrowed",
        screenIOweTitle = "I Borrowed",
        bannerYouOwe = "you owe",
        bannerOwedToYou = "owed to you",
    )
}
