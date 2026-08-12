// AGP pinned deliberately to the 8.x line.
// AGP 9.x requires Gradle 9.x and deletes the old DSL interfaces outright —
// that is the migration that broke the Unpick build. 8.10.1 supports API 36,
// which is all we need. Do not let Android Studio "helpfully" bump this.
plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
