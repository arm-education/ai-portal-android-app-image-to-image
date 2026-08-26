package com.arm.learningpath.imagetoimage.storage

enum class BundledModelState(val label: String) {
    FOUND_IN_APP_STORAGE("found in app storage"),
    BUNDLED_WITH_APP("bundled with app"),
    PLACEHOLDER_BUNDLED("placeholder bundled"),
    MISSING("missing"),
}
