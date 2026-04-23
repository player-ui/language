package com.intuit.playerui.lang.generator

import java.io.File

object TestFixtures {
    fun loadFixture(name: String): String {
        val classLoader = TestFixtures::class.java.classLoader
        val resource = classLoader.getResource("com/intuit/playerui/lang/generator/fixtures/$name")
        return if (resource != null) {
            resource.readText()
        } else {
            val file =
                File("language/generators/kotlin/src/test/kotlin/com/intuit/playerui/lang/generator/fixtures/$name")
            file.readText()
        }
    }
}
