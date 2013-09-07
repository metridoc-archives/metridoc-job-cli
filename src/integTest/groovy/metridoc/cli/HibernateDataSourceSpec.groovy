package metridoc.cli

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
class HibernateDataSourceSpec extends AbstractFunctionalSpec {

    void "localMysql and MergeConfig flags should work"() {
        when:
        int exitCode = runCommand(["--stacktrace", "src/test/testJobs/complexJob/metridoc-job-hibernate_gorm_tests", "--localMysql",
                "--mergeMetridocConfig=false"])

        then:
        0 == exitCode
    }
}
