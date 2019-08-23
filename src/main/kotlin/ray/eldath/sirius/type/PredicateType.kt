package ray.eldath.sirius.type

import ray.eldath.sirius.core.ValidationPredicate

typealias BaseValidationPredicate = ValidationPredicate<*>
typealias Predicate<T> = T.() -> Boolean