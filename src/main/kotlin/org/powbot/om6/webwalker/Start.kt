import org.powbot.webwalk.WebWalkerScript

/**
 * Entry point for standalone execution.
 */
fun main() {
    val script = WebWalkerScript()
    script.startScript("localhost", "WebWalker", false)
}