package xyz.ksharma.krail.core.testing.helpers

/**
 * An ordered record of what several fakes were asked to do, so a test can hold the order work
 * happened in. Hand the same log to each fake that takes one; every fake records its own calls.
 */
class CallLog {

    private val recorded = mutableListOf<String>()

    val entries: List<String> get() = recorded

    fun record(entry: String) {
        recorded += entry
    }

    fun count(entry: String): Int = recorded.count { it == entry }

    fun clear() {
        recorded.clear()
    }
}
