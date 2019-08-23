package ray.eldath.sirius.api

import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.JsonObjectValidationScope

typealias RootJsonObjectValidationScope = JsonObjectValidationScope

fun rootJsonObject(block: RootJsonObjectValidationScope.() -> Unit): JsonObjectValidationPredicate =
    JsonObjectValidationScope(0).apply {
        block()
        required
    }.build()