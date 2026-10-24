package com.customapplock.studio.domain

const val MASTER_PKG = "__master__"

enum class LockType(val id: String, val title: String, val emoji: String, val hint: String) {
    JIGSAW("jigsaw", "Jigsaw Puzzle", "🧩", "Drag the missing piece into 3 secret spots"),
    NUMBER_PATTERN("number_pattern", "Numbered Pattern", "🔢", "Connect numbers in your secret order. Numbers shuffle every time"),
    SCROLL_WHEEL("scroll_wheel", "Vault Wheel", "🎰", "Spin the 4 wheels to your secret code"),
    COLOR_SEQUENCE("color_sequence", "Color Sequence", "🎨", "Tap 4 glowing orbs in your secret order"),
    SHAPE_MATCH("shape_match", "Shape Match", "🔷", "Drop each shape into its secret slot"),
    CLOCK_HAND("clock_hand", "Clock Hands", "🕒", "Set the clock hands to your secret time"),
    EMOJI_MATRIX("emoji_matrix", "Emoji Matrix", "😎", "Tap 4 emojis in your secret order"),
    CALCULATOR("calculator", "Calculator", "🧮", "Type a secret formula and press ="),
    BUBBLE_POP("bubble_pop", "Bubble Pop", "🔵", "Pop 4 floating bubbles in your secret order"),
    SWIPE("swipe", "Directional Swipe", "👆", "Swipe 4 secret directions");

    companion object {
        fun fromId(id: String): LockType = entries.firstOrNull { it.id == id } ?: NUMBER_PATTERN
    }
}

enum class RelockMode(val title: String, val desc: String) {
    IMMEDIATE("Immediately on exit", "Relock the moment you leave the app"),
    SCREEN_OFF("After screen turns off", "Stay unlocked until the screen switches off"),
    GRACE("Custom grace period", "Relock after a delay you choose")
}
