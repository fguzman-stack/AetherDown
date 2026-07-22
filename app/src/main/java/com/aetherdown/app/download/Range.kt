package com.aetherdown.app.download

data class Range(
    val start: Long,
    val end: Long,
    val index: Int
) {
    val length: Long get() = end - start + 1
}
