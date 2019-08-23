@file:Suppress("MemberVisibilityCanBePrivate")

package ray.eldath.sirius.core

import ray.eldath.sirius.type.Predicate

internal object Constrains {
    internal fun <T : Comparable<T>> range(bigger: Range<T>, value: Range<T>) = RangeConstrain(bigger, value)

    internal fun <T : Comparable<T>> rangeIn(range: Range<T>, value: T) = RangeConstrain(range, value..value)

    internal fun <T : Comparable<T>> equals(expected: T, actual: T) = EqualConstrain(expected, actual)
}

internal class ContainConstrain<T>(val element: T, val container: Iterable<T>) : Constrain<T>(element, null) {
    override fun test(): Boolean = element in container
}

internal class RangeConstrain<T : Comparable<T>>(val bigger: Range<T>, val actual: Range<T>) :
    Constrain<ClosedRange<T>>(bigger, actual) {

    override fun test(): Boolean =
        bigger.start <= actual.start && bigger.endInclusive >= actual.endInclusive
}

internal class EqualConstrain<T : Comparable<T>>(val expected: T, val actual: T) : Constrain<T>(expected, actual) {
    override fun test(): Boolean = expected == actual
}

internal class LambdaConstrain<T>(val lambda: Predicate<T>, val input: T) : Constrain<Predicate<T>>(lambda, null) {
    override fun test(): Boolean = lambda.invoke(input)
}

sealed class Constrain<T>(expected: T, actual: T?) {
    abstract fun test(): Boolean
}

internal typealias AnyConstrain = Constrain<*>
internal typealias Range<T> = ClosedRange<T>