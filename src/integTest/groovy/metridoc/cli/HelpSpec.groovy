package metridoc.cli
/**
 * Created with IntelliJ IDEA on 8/14/13
 * @author Tommy Barker
 */
class HelpSpec extends AbstractFunctionalSpec {

    void "test various implementations of help"() {
        given:
        int exitCode

        when: "I run command with no args"
        exitCode = runCommand([])

        then:
        0 == exitCode
        output =~ /\s+Available Commands:\s+/
        output =~ /\s+Global Options:\s+/

        when:
        exitCode = runCommand(["-help"])

        then:
        0 == exitCode
        output =~ /\s+Available Commands:\s+/
        output =~ /\s+Global Options:\s+/

        when:
        exitCode = runCommand(["help"])

        then:
        0 == exitCode
        output =~ /\s+Available Commands:\s+/
        output =~ /\s+Global Options:\s+/
    }

    void "test help for a job"() {
        when: "I ask help for a job with a path"
        int exitCode = runCommand(["help", "src/test/testJobs/script/simpleScript.groovy"])

        then: "The readme at its base is returned"
        0 == exitCode
        output.contains("I am a simple script")
    }

    void "test help for a job after install"() {
        when: "I install a job"
        runCommand(["install", "src/test/testJobs/metridoc-job-bar-0.1.zip"])

        and: "and ask for help on installed job"
        int exitCode =runCommand(["help", "bar"])

        then:
        0 == exitCode
        output.contains("readme from bar")

        cleanup:
        new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-bar-0.1").deleteDir()
    }
}
