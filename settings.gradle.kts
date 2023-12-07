rootProject.name = "coil-root"

// https://docs.gradle.org/7.4/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Public modules
include(
    "coil",
    "coil-core",
    "coil-compose",
    "coil-compose-core",
    "coil-network",
    "coil-gif",
    "coil-svg",
    "coil-video",
    "coil-bom",
    "coil-test",
)

// Private modules
include(
    "coil-benchmark",
    "coil-test-internal",
    "coil-test-paparazzi",
    "coil-test-roborazzi",
    "samples:compose",
    "samples:shared",
    "samples:view",
)
