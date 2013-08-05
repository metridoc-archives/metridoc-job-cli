package metridoc.cli

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMainSpec extends Specification {

    void "test running a script"() {
        given:
        def args = ["src/test/testJobs/script/simpleScript.groovy"]
        def binding = new Binding()
        binding.args = args
        def main = new MetridocMain(binding: binding)

        when:
        def result = main.run()

        then:
        "simpleScript ran" == result
        noExceptionThrown()
    }

    void "test running a simple job"() {
        given:
        def args = ["src/test/testJobs/simpleJob"]
        def binding = new Binding()
        binding.args = args
        def main = new MetridocMain(binding: binding)

        when:
        def result = main.run()

        then:
        noExceptionThrown()
        "foo ran" == result
    }
}
