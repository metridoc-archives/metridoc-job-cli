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
        exitCode = runCommand([], new File(baseWorkDir))

        then:
        0 == exitCode
        output =~ /\s+Available Commands:\s+/
        output =~ /\s+Global Options:\s+/

        when:
        exitCode = runCommand(["-help"], new File(baseWorkDir))

        then:
        0 == exitCode
        output =~ /\s+Available Commands:\s+/
        output =~ /\s+Global Options:\s+/

        when:
        exitCode = runCommand(["help"], new File(baseWorkDir))

        then:
        0 == exitCode
        output =~ /\s+Available Commands:\s+/
        output =~ /\s+Global Options:\s+/
    }

    void "test help for a job"() {
        given:
        int exitCode

        when: "I ask help for a job with a path"
        exitCode = runCommand(["help", "src/test/testJobs/script/simpleScript.groovy"], new File(baseWorkDir))

        then: "The readme at its base is returned"
        0 == exitCode
        output.contains("I am a simple script")
    }
}
