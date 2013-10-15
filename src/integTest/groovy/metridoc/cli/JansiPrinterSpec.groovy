package metridoc.cli

/**
 * Created with IntelliJ IDEA on 10/15/13
 * @author Tommy Barker
 */
class JansiPrinterSpec extends AbstractFunctionalSpec {

    void "test jansi logging"() {
        when:
        int exitCode = runCommand(["src/test/testJobs/script/loggingTest.groovy"])

        then:
        0 == exitCode
        //partial ansi code for green (ie INFO)
        output.contains("[32m")

        when:
        exitCode = runCommand(["--plainText","src/test/testJobs/script/loggingTest.groovy"])

        then:
        0 == exitCode
        !output.contains("[32m")
    }
}
