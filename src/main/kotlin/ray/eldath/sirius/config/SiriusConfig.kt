package ray.eldath.sirius.config

class SiriusValidationConfig(
    val requiredByDefault: Boolean = false,
    val nullableByDefault: Boolean = false,
    val stringNonEmptyByDefault: Boolean = false,
    val stringNonBlankByDefault: Boolean = false
)