package metridoc.cli

import org.apache.commons.lang.SystemUtils

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

    void "test help for a directory based job"() {
        when:
        int exitCode = runCommand(["help", "src/test/testJobs/complexJob/metridoc-job-foo-0.1"])

        then:
        0 == exitCode
        output.contains("complex foo's README")
    }

    void "when standard help is displayed there is a new line above and below the message"() {
        when:
        int exitCode = runCommand([])

        then:
        0 == exitCode
        output.startsWith(SystemUtils.LINE_SEPARATOR)
        output.endsWith(SystemUtils.LINE_SEPARATOR)
    }

    void "help on a bad job name should return a reasonable error message"() {
        when:
        int exitCode = runCommand(["help", "asdasd"])

        then:
        exitCode > 0
        output.contains("[asdasd] is not a recognized job")
    }
}
