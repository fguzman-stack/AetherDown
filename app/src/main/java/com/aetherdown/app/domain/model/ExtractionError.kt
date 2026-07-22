package com.aetherdown.app.domain.model

sealed class ExtractionError : Exception() {
    object AgeRestricted : ExtractionError() {
        override val message: String = "This content is age restricted."
    }
    object RegionLocked : ExtractionError() {
        override val message: String = "This content is region locked."
    }
    object PlatformBroken : ExtractionError() {
        override val message: String = "Platform structure changed. Update required."
    }
    data class Unknown(val msg: String) : ExtractionError() {
        override val message: String = msg
    }
}
