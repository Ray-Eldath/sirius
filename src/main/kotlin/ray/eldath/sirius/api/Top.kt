package ray.eldath.sirius.api

import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.JsonObjectValidationScope

fun rootJsonObject(block: JsonObjectValidationScope.() -> Unit): JsonObjectValidationPredicate =
    JsonObjectValidationScope(0).apply {
        block()
        required
    }.build()