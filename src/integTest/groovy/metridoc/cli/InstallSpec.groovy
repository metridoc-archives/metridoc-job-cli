package metridoc.cli
/**
 * Created with IntelliJ IDEA on 8/15/13
 * @author Tommy Barker
 */
class InstallSpec extends AbstractFunctionalSpec {

    def bar1 = new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-bar-0.1")
    def bar2 = new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-bar-0.2")
    def simpleJobUnversioned = new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-simpleJob")
    def simpleJobVersioned = new File("${System.getProperty("user.home")}/" +
            ".metridoc/jobs/metridoc-job-simpleJob-master")

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

    void "test installing a directory"() {
        when:
        int exitCode = runCommand(["install", "src/test/testJobs/simpleJob"])

        then:
        0 == exitCode
        simpleJobUnversioned.exists()

        when: "installing it again"
        exitCode = runCommand(["install", "src/test/testJobs/simpleJob"])

        then: "old one should be deleted, new one installed"
        output.contains("upgrading metridoc-job-simpleJob")
        0 == exitCode
        simpleJobUnversioned.exists()

        when:
        exitCode = runCommand(["--stacktrace", "simpleJob", "--mergeMetridocConfig=false", "--embeddedDataSource"])

        then:
        0 == exitCode
        output.contains("foo ran")

        cleanup:
        simpleJobUnversioned.deleteDir()
    }

    void "test installing from github"() {
        when:
        int exitCode = runCommand(["install", "https://github.com/metridoc/metridoc-job-illiad/archive/master.zip"])

        then:
        0 == exitCode

        when:
        exitCode = runCommand(["install", "https://github.com/metridoc/metridoc-job-illiad/archive/master.zip"])

        then:
        0 == exitCode
        output.contains("upgrading metridoc-job-illiad")
        new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-illiad-master").exists()
    }

    void "test installing from the current directory"() {
        given:
        baseWorkDir = "src/test/testJobs/complexJob/metridoc-job-foo-0.1"

        when:
        int exitCode = runCommand(["install", "."])

        then:
        0 == exitCode

        when:
        exitCode = runCommand(["foo"])

        then:
        0 == exitCode
        output.contains("complex foo ran")

        cleanup:
        baseWorkDir = System.getProperty("user.dir")
    }

    void "versioned and unversioned jobs should overrite each other"() {
        when:
        int exitCode = runCommand(["install", "src/test/testJobs/simpleJob"])

        then:
        0 == exitCode
        simpleJobUnversioned.exists()
        !simpleJobVersioned.exists()

        when:
        exitCode = runCommand(["install", "src/test/testJobs/simpleJob-master"])

        then:
        0 == exitCode
        !simpleJobUnversioned.exists()
        simpleJobVersioned.exists()

        cleanup:
        simpleJobUnversioned.deleteDir()
        simpleJobVersioned.deleteDir()
    }
}
