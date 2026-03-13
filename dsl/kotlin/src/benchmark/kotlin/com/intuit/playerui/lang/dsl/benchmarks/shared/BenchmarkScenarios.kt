package com.intuit.playerui.lang.dsl.benchmarks.shared

import com.intuit.playerui.lang.dsl.flow.flow
import com.intuit.playerui.lang.dsl.mocks.builders.*
import com.intuit.playerui.lang.dsl.tagged.binding
import com.intuit.playerui.lang.dsl.tagged.expression

/**
 * Shared benchmark scenarios for comparing Kotlin and JavaScript DSL performance.
 * Each scenario must have an equivalent implementation in JavaScript to ensure fair comparison.
 */
object BenchmarkScenarios {

    /**
     * Simple form flow (~50 assets).
     *
     * Collection with 5 inputs (firstName, lastName, email, phone, address),
     * 2 actions (submit, cancel), navigation with 3 states, and data model.
     */
    fun simpleFormFlow() = flow {
        id = "simple-form"
        views = listOf(
            collection {
                id = "form"
                label { value = "User Form" }
                values(
                    input {
                        binding("user.firstName")
                        label { value = "First Name" }
                        placeholder = "Enter first name"
                    },
                    input {
                        binding("user.lastName")
                        label { value = "Last Name" }
                        placeholder = "Enter last name"
                    },
                    input {
                        binding("user.email")
                        label { value = "Email" }
                        placeholder = "Enter email"
                    },
                    input {
                        binding("user.phone")
                        label { value = "Phone" }
                        placeholder = "Enter phone"
                    },
                    input {
                        binding("user.address")
                        label { value = "Address" }
                        placeholder = "Enter address"
                    }
                )
                actions(
                    action {
                        value = "submit"
                        label { value = "Submit" }
                        metaData = mapOf("role" to "primary")
                    },
                    action {
                        value = "cancel"
                        label { value = "Cancel" }
                    }
                )
            }
        )
        navigation = mapOf(
            "BEGIN" to "FLOW_1",
            "FLOW_1" to mapOf(
                "startState" to "VIEW_form",
                "VIEW_form" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "form",
                    "transitions" to mapOf(
                        "submit" to "END_Done",
                        "cancel" to "END_Cancelled"
                    )
                ),
                "END_Done" to mapOf("state_type" to "END", "outcome" to "done"),
                "END_Cancelled" to mapOf("state_type" to "END", "outcome" to "cancelled")
            )
        )
        data = mapOf(
            "user" to mapOf(
                "firstName" to "",
                "lastName" to "",
                "email" to "",
                "phone" to "",
                "address" to ""
            )
        )
    }

    /**
     * Template-heavy flow (~100 assets).
     *
     * Template iterating 20 product items, each with contentCard (header with left/right assets)
     * and 2 actions. Tests template processing and nested structures.
     */
    fun templateHeavyFlow() = flow {
        id = "template-heavy"
        views = listOf(
            collection {
                id = "products"
                label { value = "Product List" }
                template(
                    data = binding<List<Any>>("products"),
                    output = "values",
                    dynamic = false
                ) {
                    collection {
                        label { value(binding<String>("products._index_.name")) }
                        values(
                            text { value(binding<String>("products._index_.price")) }
                        )
                        actions(
                            action {
                                value = "view"
                                label { value = "View Details" }
                            },
                            action {
                                value = "buy"
                                label { value = "Buy Now" }
                            }
                        )
                    }
                }
            }
        )
        navigation = mapOf(
            "BEGIN" to "FLOW_1",
            "FLOW_1" to mapOf(
                "startState" to "VIEW_products",
                "VIEW_products" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "products",
                    "transitions" to mapOf("*" to "END")
                ),
                "END" to mapOf("state_type" to "END", "outcome" to "done")
            )
        )
        data = mapOf(
            "products" to (0 until 20).map { i ->
                mapOf(
                    "name" to "Product $i",
                    "price" to "$${(i + 1) * 10}.00"
                )
            }
        )
    }

    /**
     * Switch-heavy flow (~80 assets).
     *
     * Multiple views with static and dynamic switches for conditional rendering.
     * Tests switch performance and expression evaluation.
     */
    fun switchHeavyFlow() = flow {
        id = "switch-heavy"
        views = listOf(
            collection {
                id = "locale-content"
                switch(
                    path = listOf("label"),
                    isDynamic = false
                ) {
                    case(expression<Boolean>("user.locale === 'es'"), text { value = "Español" })
                    case(expression<Boolean>("user.locale === 'fr'"), text { value = "Français" })
                    case(expression<Boolean>("user.locale === 'de'"), text { value = "Deutsch" })
                    case(expression<Boolean>("user.locale === 'ja'"), text { value = "日本語" })
                    default(text { value = "English" })
                }
                values(
                    collection {
                        switch(
                            path = listOf("label"),
                            isDynamic = true
                        ) {
                            case(expression<Boolean>("user.isPremium"), text { value = "Premium User" })
                            default(text { value = "Standard User" })
                        }
                    },
                    collection {
                        switch(
                            path = listOf("label"),
                            isDynamic = false
                        ) {
                            case(expression<Boolean>("user.age >= 18"), text { value = "Adult" })
                            case(expression<Boolean>("user.age >= 13"), text { value = "Teen" })
                            default(text { value = "Child" })
                        }
                    },
                    collection {
                        switch(
                            path = listOf("label"),
                            isDynamic = true
                        ) {
                            case(expression<Boolean>("user.verified"), text { value = "Verified" })
                            default(text { value = "Unverified" })
                        }
                    }
                )
            }
        )
        navigation = mapOf(
            "BEGIN" to "FLOW_1",
            "FLOW_1" to mapOf(
                "startState" to "VIEW_locale-content",
                "VIEW_locale-content" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "locale-content",
                    "transitions" to mapOf("*" to "END")
                ),
                "END" to mapOf("state_type" to "END", "outcome" to "done")
            )
        )
        data = mapOf(
            "user" to mapOf(
                "locale" to "en",
                "isPremium" to false,
                "age" to 25,
                "verified" to true
            )
        )
    }

    /**
     * Production-scale flow (~20k lines JSON).
     *
     * 5 views with deeply nested collections, templates, switches, and large data.
     * Mirrors a real production builder that generates ~20k lines of JSON.
     */
    fun productionScaleFlow() = flow {
        id = "production-scale"
        views = listOf(
            // View 1: Large registration form (8 groups x 5 inputs = 40 inputs)
            collection {
                id = "registration"
                label { value = "Complete Registration" }
                values(
                    *(0 until 8).map { groupIdx ->
                        val groupName = listOf(
                            "Personal", "Contact", "Address", "Employment",
                            "Education", "Financial", "Medical", "Preferences"
                        )[groupIdx]
                        val fields = listOf(
                            "field1", "field2", "field3", "field4", "field5"
                        )
                        collection {
                            label { value = "$groupName Information" }
                            values(
                                *fields.mapIndexed { fieldIdx, field ->
                                    input {
                                        binding("registration.$groupName.$field")
                                        label { value = "$groupName ${field.replaceFirstChar { it.uppercase() }}" }
                                        placeholder = "Enter $groupName $field"
                                    }
                                }.toTypedArray()
                            )
                        }
                    }.toTypedArray()
                )
                actions(
                    action {
                        value = "next"
                        label { value = "Continue" }
                        metaData = mapOf("role" to "primary")
                    },
                    action {
                        value = "save"
                        label { value = "Save Draft" }
                    },
                    action {
                        value = "cancel"
                        label { value = "Cancel" }
                    }
                )
            },
            // View 2: Product catalog (template over 50 products)
            collection {
                id = "catalog"
                label { value = "Product Catalog" }
                template(
                    data = binding<List<Any>>("products"),
                    output = "values",
                    dynamic = false
                ) {
                    collection {
                        label { value(binding<String>("products._index_.name")) }
                        values(
                            text { value(binding<String>("products._index_.description")) },
                            text { value(binding<String>("products._index_.price")) },
                            text { value(binding<String>("products._index_.sku")) }
                        )
                        actions(
                            action {
                                value = "view"
                                label { value = "View Details" }
                            },
                            action {
                                value = "add-to-cart"
                                label { value = "Add to Cart" }
                                metaData = mapOf("role" to "primary")
                            },
                            action {
                                value = "wishlist"
                                label { value = "Add to Wishlist" }
                            }
                        )
                    }
                }
            },
            // View 3: Order history (30 orders x 5 line items each)
            collection {
                id = "orders"
                label { value = "Order History" }
                template(
                    data = binding<List<Any>>("orders"),
                    output = "values",
                    dynamic = false
                ) {
                    collection {
                        label { value(binding<String>("orders._index_.orderNumber")) }
                        values(
                            text { value(binding<String>("orders._index_.status")) },
                            text { value(binding<String>("orders._index_.total")) },
                            text { value(binding<String>("orders._index_.date")) }
                        )
                        template(
                            data = binding<List<Any>>("orders._index_.items"),
                            output = "values",
                            dynamic = false
                        ) {
                            collection {
                                label { value(binding<String>("orders._index_.items._index1_.name")) }
                                values(
                                    text { value(binding<String>("orders._index_.items._index1_.quantity")) },
                                    text { value(binding<String>("orders._index_.items._index1_.price")) }
                                )
                            }
                        }
                    }
                }
            },
            // View 4: Dashboard (10 switch blocks x 4 cases + templates)
            collection {
                id = "dashboard"
                switch(
                    path = listOf("label"),
                    isDynamic = true
                ) {
                    case(expression<Boolean>("user.isPremium"), text { value = "Premium Dashboard" })
                    default(text { value = "Standard Dashboard" })
                }
                values(
                    *(0 until 10).map { sectionIdx ->
                        val sectionName = listOf(
                            "analytics", "reports", "alerts", "activity",
                            "performance", "inventory", "marketing", "support",
                            "compliance", "feedback"
                        )[sectionIdx]
                        collection {
                            switch(
                                path = listOf("label"),
                                isDynamic = false
                            ) {
                                case(expression<Boolean>("user.locale === 'es'"), text { value = "${sectionName.replaceFirstChar { it.uppercase() }} (ES)" })
                                case(expression<Boolean>("user.locale === 'fr'"), text { value = "${sectionName.replaceFirstChar { it.uppercase() }} (FR)" })
                                case(expression<Boolean>("user.locale === 'de'"), text { value = "${sectionName.replaceFirstChar { it.uppercase() }} (DE)" })
                                default(text { value = sectionName.replaceFirstChar { it.uppercase() } })
                            }
                            template(
                                data = binding<List<Any>>("dashboard.$sectionName"),
                                output = "values",
                                dynamic = true
                            ) {
                                text { value(binding<String>("dashboard.$sectionName._index_.value")) }
                            }
                        }
                    }.toTypedArray()
                )
            },
            // View 5: Settings (6 categories x 8 inputs = 48 inputs)
            collection {
                id = "settings"
                label { value = "Account Settings" }
                values(
                    *(0 until 6).map { catIdx ->
                        val category = listOf(
                            "profile", "security", "notifications",
                            "privacy", "billing", "integrations"
                        )[catIdx]
                        val settings = listOf(
                            "setting1", "setting2", "setting3", "setting4",
                            "setting5", "setting6", "setting7", "setting8"
                        )
                        collection {
                            label { value = "${category.replaceFirstChar { it.uppercase() }} Settings" }
                            values(
                                *settings.mapIndexed { settingIdx, setting ->
                                    input {
                                        binding("settings.$category.$setting")
                                        label { value = "${category.replaceFirstChar { it.uppercase() }} ${setting.replaceFirstChar { it.uppercase() }}" }
                                    }
                                }.toTypedArray()
                            )
                            actions(
                                action {
                                    value = "save-$category"
                                    label { value = "Save ${category.replaceFirstChar { it.uppercase() }}" }
                                    metaData = mapOf("role" to "primary")
                                },
                                action {
                                    value = "reset-$category"
                                    label { value = "Reset ${category.replaceFirstChar { it.uppercase() }}" }
                                }
                            )
                        }
                    }.toTypedArray()
                )
            }
        )
        navigation = mapOf(
            "BEGIN" to "FLOW_1",
            "FLOW_1" to mapOf(
                "startState" to "VIEW_registration",
                "VIEW_registration" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "registration",
                    "transitions" to mapOf("next" to "VIEW_catalog", "cancel" to "END_Cancelled")
                ),
                "VIEW_catalog" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "catalog",
                    "transitions" to mapOf("next" to "VIEW_orders", "back" to "VIEW_registration")
                ),
                "VIEW_orders" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "orders",
                    "transitions" to mapOf("next" to "VIEW_dashboard", "back" to "VIEW_catalog")
                ),
                "VIEW_dashboard" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "dashboard",
                    "transitions" to mapOf("next" to "VIEW_settings", "back" to "VIEW_orders")
                ),
                "VIEW_settings" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "settings",
                    "transitions" to mapOf("done" to "END_Done", "back" to "VIEW_dashboard")
                ),
                "END_Done" to mapOf("state_type" to "END", "outcome" to "done"),
                "END_Cancelled" to mapOf("state_type" to "END", "outcome" to "cancelled")
            )
        )
        data = mapOf(
            "registration" to listOf(
                "Personal", "Contact", "Address", "Employment",
                "Education", "Financial", "Medical", "Preferences"
            ).associateWith { group ->
                (1..5).associate { "field$it" to "" }
            },
            "products" to (0 until 50).map { i ->
                mapOf(
                    "name" to "Product $i",
                    "description" to "Description for product $i with detailed information",
                    "price" to "$${(i + 1) * 10}.00",
                    "sku" to "SKU-${1000 + i}"
                )
            },
            "orders" to (0 until 30).map { i ->
                mapOf(
                    "orderNumber" to "ORD-${2000 + i}",
                    "status" to if (i % 3 == 0) "delivered" else if (i % 3 == 1) "shipped" else "processing",
                    "total" to "$${(i + 1) * 50}.00",
                    "date" to "2026-${String.format("%02d", (i % 12) + 1)}-${String.format("%02d", (i % 28) + 1)}",
                    "items" to (0 until 5).map { j ->
                        mapOf(
                            "name" to "Item $j of Order $i",
                            "quantity" to "${j + 1}",
                            "price" to "$${(j + 1) * 15}.00"
                        )
                    }
                )
            },
            "dashboard" to listOf(
                "analytics", "reports", "alerts", "activity",
                "performance", "inventory", "marketing", "support",
                "compliance", "feedback"
            ).associateWith { section ->
                (0 until 8).map { i -> mapOf("value" to "$section metric $i") }
            },
            "settings" to listOf(
                "profile", "security", "notifications",
                "privacy", "billing", "integrations"
            ).associateWith { category ->
                (1..8).associate { "setting$it" to "" }
            },
            "user" to mapOf(
                "isPremium" to true,
                "locale" to "en"
            )
        )
    }

    /**
     * Complex nested flow (~200 assets, production-like).
     *
     * 3 views with deeply nested collections, templates, and switches.
     * Represents realistic production content with multiple features combined.
     */
    fun complexNestedFlow() = flow {
        id = "complex-nested"
        views = listOf(
            // View 1: Nested collections with inputs
            collection {
                id = "registration"
                label { value = "Registration Form" }
                values(
                    collection {
                        label { value = "Personal Information" }
                        values(
                            input {
                                binding("user.firstName")
                                label { value = "First Name" }
                            },
                            input {
                                binding("user.lastName")
                                label { value = "Last Name" }
                            },
                            input {
                                binding("user.email")
                                label { value = "Email" }
                            }
                        )
                    },
                    collection {
                        label { value = "Address" }
                        values(
                            input {
                                binding("user.address.street")
                                label { value = "Street" }
                            },
                            input {
                                binding("user.address.city")
                                label { value = "City" }
                            },
                            input {
                                binding("user.address.state")
                                label { value = "State" }
                            },
                            input {
                                binding("user.address.zip")
                                label { value = "ZIP Code" }
                            }
                        )
                    },
                    collection {
                        label { value = "Preferences" }
                        values(
                            input {
                                binding("user.preferences.newsletter")
                                label { value = "Newsletter" }
                            },
                            input {
                                binding("user.preferences.notifications")
                                label { value = "Notifications" }
                            }
                        )
                    }
                )
            },
            // View 2: Template with nested structures
            collection {
                id = "orders"
                label { value = "Your Orders" }
                template(
                    data = binding<List<Any>>("orders"),
                    output = "values",
                    dynamic = false
                ) {
                    collection {
                        label { value(binding<String>("orders._index_.orderNumber")) }
                        values(
                            text { value(binding<String>("orders._index_.product")) },
                            text { value(binding<String>("orders._index_.total")) }
                        )
                        template(
                            data = binding<List<Any>>("orders._index_.items"),
                            output = "values",
                            dynamic = false
                        ) {
                            text { value(binding<String>("orders._index_.items._index1_.name")) }
                        }
                    }
                }
            },
            // View 3: Combination of switches and templates
            collection {
                id = "dashboard"
                switch(
                    path = listOf("label"),
                    isDynamic = true
                ) {
                    case(expression<Boolean>("user.isPremium"), text { value = "Premium Dashboard" })
                    default(text { value = "Standard Dashboard" })
                }
                values(
                    collection {
                        switch(
                            path = listOf("label"),
                            isDynamic = false
                        ) {
                            case(expression<Boolean>("user.locale === 'es'"), text { value = "Notificaciones" })
                            default(text { value = "Notifications" })
                        }
                        template(
                            data = binding<List<Any>>("notifications"),
                            output = "values",
                            dynamic = true
                        ) {
                            text { value(binding<String>("notifications._index_.message")) }
                        }
                    }
                )
            }
        )
        navigation = mapOf(
            "BEGIN" to "FLOW_1",
            "FLOW_1" to mapOf(
                "startState" to "VIEW_registration",
                "VIEW_registration" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "registration",
                    "transitions" to mapOf("next" to "VIEW_orders")
                ),
                "VIEW_orders" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "orders",
                    "transitions" to mapOf("next" to "VIEW_dashboard")
                ),
                "VIEW_dashboard" to mapOf(
                    "state_type" to "VIEW",
                    "ref" to "dashboard",
                    "transitions" to mapOf("*" to "END")
                ),
                "END" to mapOf("state_type" to "END", "outcome" to "done")
            )
        )
        data = mapOf(
            "user" to mapOf(
                "firstName" to "",
                "lastName" to "",
                "email" to "",
                "address" to mapOf(
                    "street" to "",
                    "city" to "",
                    "state" to "",
                    "zip" to ""
                ),
                "preferences" to mapOf(
                    "newsletter" to false,
                    "notifications" to true
                ),
                "isPremium" to true,
                "locale" to "en"
            ),
            "orders" to (0 until 15).map { i ->
                mapOf(
                    "orderNumber" to "ORD-${1000 + i}",
                    "product" to "Product $i",
                    "total" to "$${(i + 1) * 25}.00",
                    "items" to listOf(
                        mapOf("name" to "Item 1"),
                        mapOf("name" to "Item 2"),
                        mapOf("name" to "Item 3")
                    )
                )
            },
            "notifications" to (0 until 10).map { i ->
                mapOf("message" to "Notification $i")
            }
        )
    }
}
