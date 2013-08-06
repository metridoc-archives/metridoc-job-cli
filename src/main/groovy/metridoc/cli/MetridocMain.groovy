package metridoc.cli

import groovy.io.FileType
import metridoc.utils.ArchiveMethods
import org.apache.commons.lang.SystemUtils

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMain extends Script {

    String jobPath = "${SystemUtils.USER_HOME}/.metridoc/jobs"
    def home = SystemUtils.USER_HOME
    def libDirectories = ["$home/.groovy/lib", "$home/.grails/drivers", "$home/.metridoc/lib", "$home/.metridoc/drivers"]

    @SuppressWarnings("GroovyAccessibility")
    @Override
    def run() {

        String[] args = binding.args
        assert args: "at lest one argument is required to declare what job to run"

        if(args[0] == "install") {
            assert args.size() == 2: "when installing a job, [install] requires a location"
            installJob(args[1])
            return
        }

        def file = new File(args[0])
        File metridocScript
        def loader = findHighestLevelClassLoader()
        addLibDirectories(loader)

        if (file.isFile()) {
            metridocScript = file
        }
        else if (file.isDirectory()) {
            loader.addURL(file.toURI().toURL())
            metridocScript = new File(file, "metridoc.groovy")
        }
        else {
            def jobName = "metridoc-job-${args[0]}"
            new File(jobPath).eachFile(FileType.DIRECTORIES) {
                if (it.name.startsWith(jobName)) {
                    loader.addURL(new File(it, "src/main/resources").toURI().toURL())
                    def groovyDir = new File(it, "src/main/groovy")
                    loader.addURL(groovyDir.toURI().toURL())
                    metridocScript = new File(groovyDir, "metridoc.groovy")
                }
            }

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

    void addLibDirectories(URLClassLoader classLoader) {
        libDirectories.each {
            addJarsFromDirectory(classLoader, new File(it))
        }
    }

    @SuppressWarnings("GroovyAccessibility")
    static void addJarsFromDirectory(URLClassLoader classloader, File directory) {
        if (directory.exists() && directory.isDirectory()) {
            directory.eachFile(FileType.FILES) {
                if(it.name.endsWith(".jar")) {
                    classloader.addURL(it.toURI().toURL())
                }
            }
        }
    }

    void installJob(String url) {
        def index = url.lastIndexOf("/")
        def fileName = url.substring(index + 1)
        def jobPathDir = new File("$jobPath")
        def file = new File(jobPathDir, fileName)
        new URL(url).withInputStream { inputStream ->
            file.newOutputStream() << inputStream
        }
        ArchiveMethods.unzip(file, jobPathDir)
        def filesToDelete = []

        jobPathDir.eachFile {
            if(it.isFile() && it.name.endsWith(".zip")) {
                filesToDelete << it
            }
        }

        filesToDelete.each {
            it.delete()
        }
    }
}
