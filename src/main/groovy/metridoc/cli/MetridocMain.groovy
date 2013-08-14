package metridoc.cli

import groovy.io.FileType
import metridoc.utils.ArchiveMethods

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMain {

    def home = System.getProperty("user.home")
    String jobPath = "$home/.metridoc/jobs"
    def libDirectories = ["$home/.groovy/lib", "$home/.grails/drivers", "$home/.metridoc/lib", "$home/.metridoc/drivers"]
    String[] args

    public static void main(String[] args) {
        new MetridocMain(args: args).run()
    }

    @SuppressWarnings(["GroovyAccessibility", "GroovyAssignabilityCheck"])
    def run() {

        def (OptionAccessor options, CliBuilder cli) = parseArgs()

        if(doHelp(cli, options)) return

        checkForAndInstallDependencies()

        if(callingInstallDepsCommand(options)) {
            return
        }

        def cliArgs = options.arguments()

        def command = cliArgs[0]
        if(command == "install") {
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

    static boolean doHelp(CliBuilder cli, OptionAccessor options) {
        def arguments = options.arguments()
        if(arguments[0] == "help" &&  arguments.size() > 1) {
            def file = new File(arguments[1])
            def parentDir = file.parentFile
            def readme = new File(parentDir, "README")
            println readme.text

            return true
        }

        if(askingForHelp(options)) {
            cli.usage()
            return true
        }

        return false
    }

    protected static boolean callingInstallDepsCommand(OptionAccessor options) {
        options.arguments().contains("install-deps")
    }

    protected void checkForAndInstallDependencies() {
        if (!dependenciesExist()) {
            new InstallMdoc(binding: new Binding(args:args)).run()
        }
    }

    protected static boolean askingForHelp(OptionAccessor options) {
        !options.arguments() || options.help || options.arguments().contains("help")
    }

    protected List parseArgs() {
        def cli = new CliBuilder(
                usage: "mdoc [<command> | <job> | help | help <job>] [options]",
                header: "\nGlobal Options:",
                footer: "\nAvailable Commands:\n" +
                        " --> list-jobs                  lists all available jobs\n" +
                        " --> install <destination>      installs a job\n" +
                        " --> help [job name]            prints README of job, or this message job name is blank\n" +
                        " --> install-deps               installs dependencies if they are not there"
        )

        cli.help("prints this message")
        cli.stacktrace("prints full stacktrace on error")
        def options = cli.parse(args)
        [options, cli]
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

    private static boolean dependenciesExist() {
        dependenciesExistHelper("org.springframework.context.ApplicationContext")
    }


    private static boolean dependenciesExistHelper(String className) {
        try {
            Class.forName(className)
            return true
        }
        catch (ClassNotFoundException ignored) {
            return false
        }
    }
}
