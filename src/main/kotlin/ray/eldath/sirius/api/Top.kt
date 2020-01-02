package ray.eldath.sirius.api

import ray.eldath.sirius.config.SiriusValidationConfig
import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.JsonObjectValidationScope

// checkpoint
fun rootJsonObject(
    requiredByDefault: Boolean = true,
    nullableByDefault: Boolean = false,
    stringNonEmptyByDefault: Boolean = false,
    stringNonBlankByDefault: Boolean = false,
    block: JsonObjectValidationScope.() -> Unit
): JsonObjectValidationPredicate =
    SiriusValidationConfig(requiredByDefault, nullableByDefault, stringNonEmptyByDefault, stringNonBlankByDefault).let {
        JsonObjectValidationScope(0, it).apply { required }.apply(block).build()
    }