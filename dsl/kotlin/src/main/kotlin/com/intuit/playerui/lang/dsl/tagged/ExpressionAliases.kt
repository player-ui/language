package com.intuit.playerui.lang.dsl.tagged

/**
 * Comparison aliases as functions.
 */
fun <T> eq(left: Any, right: T): Expression<Boolean> = equal(left, right)

fun <T> strictEq(left: Any, right: T): Expression<Boolean> = strictEqual(left, right)

fun <T> neq(left: Any, right: T): Expression<Boolean> = notEqual(left, right)

fun <T> strictNeq(left: Any, right: T): Expression<Boolean> = strictNotEqual(left, right)

fun gt(left: Any, right: Any): Expression<Boolean> = greaterThan(left, right)

fun gte(left: Any, right: Any): Expression<Boolean> = greaterThanOrEqual(left, right)

fun lt(left: Any, right: Any): Expression<Boolean> = lessThan(left, right)

fun lte(left: Any, right: Any): Expression<Boolean> = lessThanOrEqual(left, right)

/**
 * Arithmetic aliases as functions.
 */
fun plus(vararg args: Any): Expression<Number> = add(*args)

fun minus(left: Any, right: Any): Expression<Number> = subtract(left, right)

fun times(vararg args: Any): Expression<Number> = multiply(*args)

fun div(left: Any, right: Any): Expression<Number> = divide(left, right)

fun mod(left: Any, right: Any): Expression<Number> = modulo(left, right)

/**
 * Control flow aliases as functions.
 */
fun <T> ternary(condition: Any, ifTrue: T, ifFalse: T): Expression<T> = conditional(condition, ifTrue, ifFalse)

fun <T> ifElse(condition: Any, ifTrue: T, ifFalse: T): Expression<T> = conditional(condition, ifTrue, ifFalse)
