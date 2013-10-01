package metridoc.cli

import spock.lang.IgnoreRest

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
        int exitCode = runCommand([scriptLocation, "--mergeMetridocConfig=false", "--embeddedDataSource"])

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

    void "run a job with arguments"() {
        when:
        int exitCode = runCommand(["bar", "foo", "bar"])

        then:
        0 == exitCode
        output.contains("bar has args [foo, bar]")
    }

    void "run a job in a directory with non standard root script when in the same directory"() {

        setup:
        baseWorkDir = "src/test/testJobs/complexJob/metridoc-job-foo-0.1"

        when:
        int exitCode = runCommand(["."])

        then:
        0 == exitCode
        output.contains("complex foo ran")

        cleanup:
        baseWorkDir = System.getProperty("user.dir")
    }

    void "a bad job name should return a reasonable message"() {
        when:
        int exitCode = runCommand(["--stacktrace", "asdasd"])

        then:
        3 == exitCode
        output.contains("[asdasd] is not a recognized job")
    }

    void "run a simple job from a directory"() {
        when:
        int exitCode = runCommand(["--stacktrace", "src/test/testJobs/simpleJob", "--embeddedDataSource"])

        then:
        0 == exitCode
    }

    void "run a remote script"() {
        when:
        int exitCode = runCommand(["--stacktrace", "https://raw.github.com/metridoc/metridoc-job-cli/master/src/test/testJobs/script/simpleScript.groovy", "--embeddedDataSource", "--mergeMetridocConfig=false"])

        then:
        0 == exitCode
        output.contains("simpleScript ran")
    }

    void "test job with global properties"() {
        when:
        int exitCode = runCommand(["-logLevel", "debug", "src/test/testJobs/script/injectionWithGlobalProps.groovy", "-bar=foo", "-foo", "-stacktrace"])

        then:
        0 == exitCode
    }
}
