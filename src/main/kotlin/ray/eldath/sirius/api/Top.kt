package ray.eldath.sirius.api

import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.JsonObjectValidationScope

// checkpoint
fun rootJsonObject(block: JsonObjectValidationScope.() -> Unit): JsonObjectValidationPredicate =
    JsonObjectValidationScope(0).apply { required }.apply(block).build()