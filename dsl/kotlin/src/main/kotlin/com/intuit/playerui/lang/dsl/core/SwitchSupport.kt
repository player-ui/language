package com.intuit.playerui.lang.dsl.core

import com.intuit.playerui.lang.dsl.FluentDslMarker
import com.intuit.playerui.lang.dsl.tagged.Expression

/**
 * Typed condition for switch cases.
 */
sealed interface SwitchCondition {
    data class Static(
        val value: Boolean,
    ) : SwitchCondition

    data class Dynamic(
        val expr: Expression<Boolean>,
    ) : SwitchCondition
}

/**
 * Arguments for switch configuration.
 */
data class SwitchArgs(
    val cases: List<SwitchCase>,
    val isDynamic: Boolean = false,
)

/**
 * A single case in a switch configuration.
 */
data class SwitchCase(
    val condition: SwitchCondition,
    val asset: FluentBuilder<*>,
)

/**
 * Internal metadata for switch configurations.
 */
internal data class SwitchMetadata(
    val path: List<Any>,
    val args: SwitchArgs,
)

/**
 * Helper builder for constructing switch cases.
 */
@FluentDslMarker
class SwitchBuilder {
    internal val cases = mutableListOf<SwitchCase>()

    /**
     * Adds a case with a boolean condition.
     */
    fun case(
        condition: Boolean,
        asset: FluentBuilder<*>,
    ) {
        cases.add(SwitchCase(SwitchCondition.Static(condition), asset))
    }

    /**
     * Adds a case with an expression condition.
     */
    fun case(
        condition: Expression<Boolean>,
        asset: FluentBuilder<*>,
    ) {
        cases.add(SwitchCase(SwitchCondition.Dynamic(condition), asset))
    }

    /**
     * Adds a default case (always true).
     */
    fun default(asset: FluentBuilder<*>) {
        cases.add(SwitchCase(SwitchCondition.Static(true), asset))
    }

    /**
     * Adds a default case using a DSL block.
     */
    fun default(init: () -> FluentBuilder<*>) {
        cases.add(SwitchCase(SwitchCondition.Static(true), init()))
    }
}
