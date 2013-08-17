package metridoc.cli

/**
 * Created with IntelliJ IDEA on 8/16/13
 * @author Tommy Barker
 */
class RunJobSpec extends AbstractFunctionalSpec {

    def scriptLocation = "src/test/testJobs/script/simpleScript.groovy"

    void "test running a script"() {
        when:
        int exitCode = runCommand([scriptLocation])

        then:
        0 == exitCode
        output.contains("simpleScript ran")
    }
}
