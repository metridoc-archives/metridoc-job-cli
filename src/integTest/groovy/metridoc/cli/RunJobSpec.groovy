package metridoc.cli

/**
 * Created with IntelliJ IDEA on 8/16/13
 * @author Tommy Barker
 */
class RunJobSpec extends AbstractFunctionalSpec {

    def scriptLocation = "src/test/testJobs/script/simpleScript.groovy"

    def setup() {
        runCommand(["install", "src/test/testJobs/metridoc-job-bar-0.1.zip"])
    }

    def cleanup() {
        assert new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-bar-0.1").deleteDir()
    }

    void "test running a script"() {
        when:
        int exitCode = runCommand([scriptLocation])

        then:
        0 == exitCode
        output.contains("simpleScript ran")
    }

    void "run a complex job"() {
        when:
        int exitCode = runCommand(["bar"])

        then:
        0 == exitCode
        output.contains("bar has run")
    }
}
