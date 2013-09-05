package metridoc.cli

/**
 * @author Tommy Barker
 */
class ListJobsSpec extends AbstractFunctionalSpec {

    void "test list jobs"() {
        when:
        runCommand(["install", "src/test/testJobs/metridoc-job-bar-0.1.zip"])
        int exitCode = runCommand(["list-jobs"])

        then:
        0 == exitCode
        output.contains("Available Jobs:")
        output.contains(" --> bar (v0.1)")
    }

    void "test installing a job with no version"() {
        when:
        runCommand(["install", "src/test/testJobs/simpleJob"])
        int exitCode = runCommand(["list-jobs"])

        then:
        0 == exitCode
        output.contains("Available Jobs:")
        output.contains(" --> simpleJob")
    }
}
