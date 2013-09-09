package metridoc.cli

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
class GormSpec extends AbstractFunctionalSpec {

    void "basic gorm test"() {
        when:
        int exitCode = runCommand(["--stacktrace", "src/test/testJobs/complexJob/metridoc-job-gorm",
                "--mergeMetridocConfig=false"])

        then:
        0 == exitCode
    }
}
