package metridoc.cli

import spock.lang.Timeout

/**
 * @author Tommy Barker
 */
class InstallDepsSpec extends AbstractFunctionalSpec {

    @Timeout(60)
    void "test install deps"() {
        given:
        int exitCode

        when: "install-deps is called"
        exitCode = runCommand(["install-deps"], new File(baseWorkDir))

        then:
        0 == exitCode
        5 < new File("${System.getProperty('user.dir')}/build/install/mdoc/lib").list().size()

        when: "install-deps is called again"
        exitCode = runCommand(["install-deps"], new File(baseWorkDir))

        then:
        0 == exitCode
        output.contains("Dependencies have already been installed")
    }
}
