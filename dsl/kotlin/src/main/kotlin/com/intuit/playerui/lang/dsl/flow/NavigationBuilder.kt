package com.intuit.playerui.lang.dsl.flow

import com.intuit.playerui.lang.dsl.FluentDslMarker
import com.intuit.playerui.lang.dsl.tagged.Expression

/**
 * Type-safe builder for Player-UI navigation state machines.
 *
 * Example:
 * ```kotlin
 * navigation {
 *     begin = "FLOW_1"
 *     flow("FLOW_1") {
 *         startState = "VIEW_welcome"
 *         view("VIEW_welcome", ref = "welcome-views-0") {
 *             on("next", "END_Done")
 *         }
 *         end("END_Done", outcome = "done")
 *     }
 * }
 * ```
 */
@FluentDslMarker
class NavigationBuilder {
    var begin: String = "FLOW_1"
    private val flows = mutableMapOf<String, NavigationFlowBuilder>()

    fun flow(
        name: String,
        init: NavigationFlowBuilder.() -> Unit,
    ) {
        flows[name] = NavigationFlowBuilder().apply(init)
    }

    fun build(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>("BEGIN" to begin)
        flows.forEach { (name, builder) ->
            result[name] = builder.build()
        }
        return result
    }
}

@FluentDslMarker
class NavigationFlowBuilder {
    var startState: String = ""
    var onStart: Expression<*>? = null
    var onEnd: Expression<*>? = null
    private val states = mutableMapOf<String, Map<String, Any?>>()

    fun view(
        name: String,
        ref: String,
        init: ViewStateBuilder.() -> Unit = {},
    ) {
        states[name] = ViewStateBuilder(ref).apply(init).build()
    }

    fun end(
        name: String,
        outcome: String,
    ) {
        states[name] =
            mapOf(
                "state_type" to "END",
                "outcome" to outcome,
            )
    }

    fun action(
        name: String,
        exp: Expression<*>,
        init: ActionStateBuilder.() -> Unit = {},
    ) {
        states[name] = ActionStateBuilder(exp).apply(init).build()
    }

    fun build(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        if (startState.isNotEmpty()) result["startState"] = startState
        onStart?.let { result["onStart"] = it.toString() }
        onEnd?.let { result["onEnd"] = it.toString() }
        result.putAll(states)
        return result
    }
}

@FluentDslMarker
class ViewStateBuilder(
    private val ref: String,
) {
    private val transitions = mutableMapOf<String, String>()

    fun on(
        event: String,
        target: String,
    ) {
        transitions[event] = target
    }

    fun build(): Map<String, Any?> {
        val result =
            mutableMapOf<String, Any?>(
                "state_type" to "VIEW",
                "ref" to ref,
            )
        if (transitions.isNotEmpty()) {
            result["transitions"] = transitions.toMap()
        }
        return result
    }
}

@FluentDslMarker
class ActionStateBuilder(
    private val exp: Expression<*>,
) {
    private val transitions = mutableMapOf<String, String>()

    fun on(
        event: String,
        target: String,
    ) {
        transitions[event] = target
    }

    fun build(): Map<String, Any?> {
        val result =
            mutableMapOf<String, Any?>(
                "state_type" to "ACTION",
                "exp" to exp.toString(),
            )
        if (transitions.isNotEmpty()) {
            result["transitions"] = transitions.toMap()
        }
        return result
    }
}

/**
 * Extension function on FlowBuilder to add type-safe navigation.
 */
fun FlowBuilder.navigation(init: NavigationBuilder.() -> Unit) {
    navigation = NavigationBuilder().apply(init).build()
}
