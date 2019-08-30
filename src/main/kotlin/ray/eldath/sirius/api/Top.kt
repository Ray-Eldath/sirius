package ray.eldath.sirius.api

import ray.eldath.sirius.config.SiriusValidationConfig
import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.JsonObjectValidationScope

// checkpoint
fun rootJsonObject(
    requiredByDefault: Boolean = false,
    block: JsonObjectValidationScope.() -> Unit
): JsonObjectValidationPredicate =
    JsonObjectValidationScope(0, SiriusValidationConfig(requiredByDefault)).apply { required }.apply(block).build()