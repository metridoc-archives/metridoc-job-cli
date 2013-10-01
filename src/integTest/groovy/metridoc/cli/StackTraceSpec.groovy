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

    void "stacktrace flag should be injectable"() {
        when: "stacktrace is an mdoc argument"
        int exitCode = runCommand(["--stacktrace", "src/test/testJobs/script/injectableStackTrace.groovy"])

        then:
        0 == exitCode
        output.contains("stacktrace is injectable")

        when: "stacktrace is a job argument"
        exitCode = runCommand(["src/test/testJobs/script/injectableStackTrace.groovy", "--stacktrace"])

        then:
        0 == exitCode
        output.contains("stacktrace is injectable")
    }

}
