package metridoc.cli

import spock.lang.Specification

/**
 * @author Tommy Barker
 */
class InstallMdocDependenciesSpec extends Specification {

    void "test retrieving file name from url"() {
        when:
        def fileName = InstallMdocDependencies.getFileName("http://jcenter.bintray.com/org/grails/grails-spring/2.2.3/grails-spring-2.2.3.jar")

        then:
        "grails-spring-2.2.3.jar" == fileName
    }
}
