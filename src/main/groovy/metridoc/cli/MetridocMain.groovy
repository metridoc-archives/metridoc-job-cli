package metridoc.cli

import org.apache.commons.lang.SystemUtils

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMain extends Script {

    String jobPath = "${SystemUtils.USER_HOME}/.metridoc/jobs"

    @SuppressWarnings("GroovyAccessibility")
    @Override
    def run() {
        String[] args = binding.args
        assert args: "at lest one argument is required to declare what job to run"
        def file = new File(args[0])
        File metridocScript
        if (file.isFile()) {
            metridocScript = file
        }
        else if (file.isDirectory()) {
            findHighestLevelClassLoader().addURL(file.toURI().toURL())
            metridocScript = new File(file, "metridoc.groovy")
        }

        return new GroovyShell().evaluate(metridocScript)
    }

    URLClassLoader findHighestLevelClassLoader() {
        def loader = this.class.classLoader

        if (loader.rootLoader) {
            return this.class.classLoader.rootLoader as URLClassLoader
        }

        def loaders = []
        loaders << loader
        while (loader.parent) {
            loaders << loader.parent
            loader = loader.parent
        }
        loaders = loaders.reverse()

        for (it in loaders) {
            if (it instanceof URLClassLoader) {
                return it
            }
        }

        throw new RuntimeException("Could not find a suitable classloader")
    }
}
