package metridoc.cli

import groovy.io.FileType
import metridoc.utils.ArchiveMethods

/**
 * Created with IntelliJ IDEA on 8/5/13
 * @author Tommy Barker
 */
class MetridocMain {

    public static final String LONG_JOB_PREFIX = "metridoc-job-"
    def home = System.getProperty("user.home")
    String jobPath = "$home/.metridoc/jobs"
    def libDirectories = ["$home/.groovy/lib", "$home/.grails/drivers", "$home/.metridoc/lib", "$home/.metridoc/drivers"]
    String[] args

    public static void main(String[] args) {
        new MetridocMain(args: args).run()
    }

    @SuppressWarnings(["GroovyAccessibility", "GroovyAssignabilityCheck", "GroovyUnnecessaryReturn"])
    def run() {

        def (OptionAccessor options, CliBuilder cli) = parseArgs()

        if(doHelp(cli, options)) return

        if(doListJobs(options)) return

        checkForAndInstallDependencies(options)

        if(doInstallDeps(options)) return

        if(doInstall(options)) return

        return runJob(options)
    }

    boolean doListJobs(OptionAccessor options) {
        def cliArgs = options.arguments()
        if("list-jobs" == cliArgs[0]) {
            println "Available Jobs:"
            new File(jobPath).eachFile(FileType.DIRECTORIES) {
                def m = it.name =~ /metridoc-job-(\w+)-(.+)/
                if(m.matches()) {
                    def name = m.group(1)
                    def version = m.group(2)
                    println " --> $name (v$version)"
                }
            }

            return true
        }

        return false
    }

    @SuppressWarnings("GroovyAccessibility")
    def protected runJob(OptionAccessor options) {
        def shortJobName = options.arguments()[0]
        def file = new File(shortJobName)
        File metridocScript
        def loader = findHighestLevelClassLoader()
        addLibDirectories(loader)

        if (file.isFile()) {
            metridocScript = file
        }
        else if (file.isDirectory()) {
            addDirectoryResourcesToClassPath(loader, file)
            metridocScript = getRootScriptFromDirectory(file)
        }
        else {
            def jobDir = getJobDir(shortJobName)
            addDirectoryResourcesToClassPath(loader, jobDir)
            metridocScript = getRootScriptFromDirectory(jobDir, shortJobName)
        }

        def binding = new Binding()
        binding.args = [] as String[]
        if(args.size() > 2) {
            def jobArgs = args[2..args.size() - 1] as String[]
            binding.args = jobArgs
        }

        assert metridocScript && metridocScript.exists() : "root script does not exist"
        return new GroovyShell(Thread.currentThread().contextClassLoader, binding).evaluate(metridocScript)
    }

    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyAccessibility"])
    protected void addDirectoryResourcesToClassPath(URLClassLoader loader, File file) {
        def resourceDir = new File(file, "src/main/resources")
        if(resourceDir.exists()) {
            loader.addURL(resourceDir.toURI().toURL())
        }
        def groovyDir = new File(file, "src/main/groovy")
        if(groovyDir.exists()) {
            loader.addURL(groovyDir.toURI().toURL())
        }

        loader.addURL(file.toURI().toURL())
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected File getRootScriptFromDirectory(File directory, String shortName = null) {
        if(shortName == null) {
            shortName = getShortName(directory.name)
        }

        def response

        response = getFileFromDirectory(directory, "metridoc.groovy")
        if(response) return response

        response = getFileFromDirectory(directory, "${shortName}.groovy")
        if(response) return response

        return response
    }

    protected static File getFileFromDirectory(File directory, String fileName) {

        def response

        response = new File(directory, fileName)
        if(response.exists()) {
            return response
        }

        def groovyDir = new File(directory, "src/main/groovy")
        if(groovyDir.exists()) {
            response = new File(groovyDir, fileName)
            if(response.exists()) {
                return response
            }
        }

        def resourcesDir = new File(directory, "src/main/resources")
        if(resourcesDir.exists()) {
            response = new File(resourcesDir, fileName)
            if(response.exists()) {
                return response
            }
        }

        return null
    }

    protected static String getShortName(String longJobName) {
        if(longJobName.startsWith(LONG_JOB_PREFIX)) {
            def shortName = longJobName.substring(LONG_JOB_PREFIX.size())
            def index = shortName.lastIndexOf("-")
            if(index != -1) {
                return shortName.substring(0, index)
            }

            return shortName
        }
        return longJobName
    }

    boolean doInstall(OptionAccessor options) {
        def cliArgs = options.arguments()

        def command = cliArgs[0]
        if(command == "install") {
            assert cliArgs.size() == 2: "when installing a job, [install] requires a location"
            installJob(cliArgs[1])
            return true
        }

        return false
    }

    protected File getJobDir(String jobName) {
        def fullJobName = jobName
        if(!fullJobName.startsWith("metridoc-job-")) {
            fullJobName = "metridoc-job-$jobName"
        }
        File jobDir = null
        new File(jobPath).eachFile(FileType.DIRECTORIES) {
            if(it.name.startsWith(fullJobName)) {
                jobDir = it
            }
        }

        return jobDir
    }

    protected boolean doHelp(CliBuilder cli, OptionAccessor options) {
        def arguments = options.arguments()
        File readme
        if(arguments[0] == "help" &&  arguments.size() > 1) {
            def jobName = arguments[1]
            def file = new File(jobName)
            def jobDir
            if (file.exists()) {
                if (file.isFile()) {
                    jobDir = file.parentFile
                }
                else {
                    jobDir = file
                }
            }
            else {
                jobDir = getJobDir(arguments[1])
            }

            readme = getFileFromDirectory(jobDir, "README")
            if (readme) {
                println readme.text
            }
            else {
                println "README does not exist for $jobName"
            }
            return true
        }

        if(askingForHelp(options)) {
            cli.usage()
            return true
        }

        return false
    }

    protected static boolean doInstallDeps(OptionAccessor options) {
        options.arguments().contains("install-deps")
    }

    protected void checkForAndInstallDependencies(OptionAccessor options) {
        if (!dependenciesExist()) {
            new InstallMdoc(binding: new Binding(args:args)).run()
        }
        else if(doInstallDeps(options)) {
            println "Dependencies have already been installed"
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
        if(!jobPathDir.exists()) {
            jobPathDir.mkdirs()
        }

        def m = fileName =~ /(metridoc-job-\w+)-[0-9]/
        if(m.lookingAt()) {
            jobPathDir.eachFile(FileType.DIRECTORIES) {
                def unversionedName = m.group(1)
                if(it.name.startsWith(unversionedName)) {
                    println "deleting $it and installing $fileName"
                    assert it.deleteDir() : "Could not delete $it"
                }
            }
        }

        def file = new File(jobPathDir, fileName)
        def fileToInstall

        try {
            fileToInstall = new URL(url)
        }
        catch (Throwable ignored) {
            fileToInstall = new File(url)
            assert fileToInstall.exists() : "$fileToInstall does not exist"
        }

        fileToInstall.withInputStream { inputStream ->
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
