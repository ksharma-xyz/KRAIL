package xyz.ksharma.krail.core.snapshot

/**
 * Annotation to mark @Preview composables for snapshot testing.
 * Only previews with this annotation will generate screenshots.
 *
 * Usage:
 * ```
 * @ScreenshotTest
 * @Preview
 * @Composable
 * fun MyComponentPreview() {
 *     PreviewTheme {
 *         MyComponent()
 *     }
 * }
 * ```
 *
 * @param threshold Optional comparison threshold (0.0 to 1.0). Default is 0.0 (exact match).
 *                  Use higher values (e.g., 0.01) for components with animations or slight variations.
 * @param description Optional description for documentation and debugging.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScreenshotTest(
    /**
     * Comparison threshold (0.0 to 1.0)
     * - 0.0 = exact match (default)
     * - 0.01 = allow 1% difference
     * - 0.05 = allow 5% difference
     */
    val threshold: Double = 0.0,

    /**
     * Description for documentation and debugging
     */
    val description: String = "",
)
