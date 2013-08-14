package metridoc.cli

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.context.ApplicationContext
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMainSpec extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    void "test running a script"() {
        given:
        def args = ["src/test/testJobs/script/simpleScript.groovy"]
        def main = new MetridocMain(args: args)

        when:
        def result = main.run()

        then:
        "simpleScript ran" == result
        noExceptionThrown()
    }

    void "test running a simple job"() {
        given:
        def args = ["src/test/testJobs/simpleJob"]
        def main = new MetridocMain(args: args)

        when:
        def result = main.run()

        then:
        noExceptionThrown()
        "foo ran" == result
    }

    void "test running a complex job"() {
        given:
        def args = ["foo"]
        def main = new MetridocMain(args: args, jobPath: "src/test/testJobs/complexJob")

        when:
        def result = main.run()

        then:
        noExceptionThrown()
        "complex foo project ran" == result
    }

    void "test installing and running a job"() {
        given:
        def args = ["install", new File("src/test/testJobs/metridoc-job-bar-0.1.zip").toURI().toURL().toString()]
        def main = new MetridocMain(args: args, jobPath: folder.getRoot().toString())

        when:
        main.run()

        then:
        noExceptionThrown()
        folder.root.listFiles().find {it.name == "metridoc-job-bar-0.1"}
        1 == folder.root.listFiles().size()
    }

    void "test check for whether or not we should install dependencies"() {
        given:
        boolean answer

        when:
        answer = MetridocMain.dependenciesExistHelper("java.lang.String")

        then:
        answer

        when:
        answer = MetridocMain.dependenciesExistHelper("foo.bar.DoesNotExist")

        then:
        !answer

        when: "calling installDependencies by default"
        answer = new MetridocMain().dependenciesExist()

        then: "the answer should be false since all dependencies will be available during unit tests"
        answer
    }
}
