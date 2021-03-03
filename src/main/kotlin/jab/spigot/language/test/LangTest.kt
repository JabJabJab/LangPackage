package jab.spigot.language.test

import jab.spigot.language.LangPackage
import org.bukkit.entity.Player

/**
 * TODO: Document.
 *
 * @author Jab
 *
 * @property name The name of the test.
 */
abstract class LangTest(val name: String) {

    /**
     * Executes the test procedure.
     *
     * @param pkg The LangPackage instance to test.
     * @param player The player running the test.
     *
     * @return Returns true if the test succeeds. Returns false if the test fails.
     */
    fun test(pkg: LangPackage, player: Player): Boolean {

        fun fail(reason: String) {
            System.err.println("""Failed to run test: "$name". Reason: "$reason".""")
        }

        try {
            println("""Running test: "$name".. """)
            if (!run(pkg, player)) {
                fail("Test Failure.")
                return false
            }
            println("""Test "$name" successful.""")
            return true
        } catch (e: Exception) {
            fail("Exception occurred.")
            e.printStackTrace(System.err)
        }

        return false
    }

    /**
     * @param pkg The langPackage instance to test.
     * @param player The player running the test.
     */
    protected abstract fun run(pkg: LangPackage, player: Player): Boolean
}