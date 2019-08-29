@file:Suppress("MemberVisibilityCanBePrivate")

package ray.eldath.sirius.core

import ray.eldath.sirius.type.Predicate

internal object Asserts {
    internal fun <T : Comparable<T>> range(bigger: Range<T>, value: Range<T>) = RangeAssert(bigger, value)

    internal fun <T : Comparable<T>> rangeIn(range: Range<T>, value: T) = RangeAssert(range, value..value)

    internal fun <T : Comparable<T>> equals(expected: T, actual: T) = EqualAssert(expected, actual)
}

internal class ContainAssert<T>(val element: T, val container: Iterable<T>) : Assert<T>() {
    override fun test(): Boolean = element in container
}

internal class RangeAssert<T : Comparable<T>>(val bigger: Range<T>, val actual: Range<T>) : Assert<ClosedRange<T>>() {

    override fun test(): Boolean =
        bigger.start <= actual.start && bigger.endInclusive >= actual.endInclusive
}

internal class EqualAssert<T : Comparable<T>>(val expected: T, val actual: T) : Assert<T>() {
    override fun test(): Boolean = expected == actual
}

internal class AssertWrapper<T>(
    val asserts: List<AnyAssert> = emptyList(),
    val tests: List<Predicate<T>> = emptyList()
)
// may be not as a assert?
// I wrote this due to I was too sleepy at that time. Apologize.

// internal class LambdaAssert<T>(val lambda: Predicate<T>, val input: T) : Assert<Predicate<T>>(lambda, null) {
//     override fun test(): Boolean = lambda.invoke(input)
// }

sealed class Assert<T> {
    abstract fun test(): Boolean
}

internal typealias AnyAssert = Assert<*>
internal typealias Range<T> = ClosedRange<T>