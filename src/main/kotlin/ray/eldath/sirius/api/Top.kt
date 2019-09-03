package ray.eldath.sirius.api

import ray.eldath.sirius.config.SiriusValidationConfig
import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.JsonObjectValidationScope

// checkpoint
fun rootJsonObject(
    requiredByDefault: Boolean = false,
    nullableByDefault: Boolean = false,
    block: JsonObjectValidationScope.() -> Unit
): JsonObjectValidationPredicate =
    SiriusValidationConfig(requiredByDefault, nullableByDefault).let {
        JsonObjectValidationScope(0, it).apply { required }.apply(block).build()
    }