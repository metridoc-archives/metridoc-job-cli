package metridoc.cli
/**
 * Created with IntelliJ IDEA on 8/15/13
 * @author Tommy Barker
 */
class InstallSpec extends AbstractFunctionalSpec {

    def bar = new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-bar-0.1")

    void "test install job"() {
        when:
        int exitCode = runCommand(["install", "src/test/testJobs/metridoc-job-bar-0.1.zip"])

        then:
        0 == exitCode
        bar.exists()

        cleanup:
        bar.deleteDir()
    }
}
