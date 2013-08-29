package metridoc.cli

/**
 * Created with IntelliJ IDEA on 8/29/13
 * @author Tommy Barker
 */
class StackTraceSpec extends AbstractFunctionalSpec {

    void "by default just the error message is printed when a job has an error"() {
        when:
        int exitCode = runCommand(["src/test/testJobs/script/errorScript.groovy"])

        then:
        exitCode == 1
        output.contains("oops!")
        !output.contains("Exception")
    }

}
