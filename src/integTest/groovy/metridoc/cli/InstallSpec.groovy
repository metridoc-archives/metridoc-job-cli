package metridoc.cli
/**
 * Created with IntelliJ IDEA on 8/15/13
 * @author Tommy Barker
 */
class InstallSpec extends AbstractFunctionalSpec {

    def bar1 = new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-bar-0.1")
    def bar2 = new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-bar-0.2")

    void "test install job"() {
        when:
        int exitCode = runCommand(["install", "src/test/testJobs/metridoc-job-bar-0.1.zip"])

        then:
        0 == exitCode
        bar1.exists()

        when:
        exitCode = runCommand(["install", "src/test/testJobs/metridoc-job-bar-0.2.zip"])

        then:
        0 == exitCode
        !bar1.exists()
        bar2.exists()

        cleanup:
        bar1.deleteDir()
        bar2.deleteDir()
    }
}
