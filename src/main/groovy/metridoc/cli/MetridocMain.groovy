package metridoc.cli

import groovy.io.FileType
import metridoc.core.MetridocScript
import metridoc.core.tools.SimpleLogTool
import metridoc.utils.ArchiveMethods
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.SystemUtils
import org.slf4j.LoggerFactory

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
        try {

            setPropertyValues(options)

            if (doHelp(cli, options)) return

            if (doListJobs(options)) return

            checkForAndInstallDependencies(options)

            if (doInstallDeps(options)) return

            if (doInstall(options)) return

            return runJob(options)
        }
        catch (Throwable ignored) {
            if (options.stacktrace) {
                throw ignored //just rethrow it
            }
            println ""
            System.err.println("ERROR: $ignored.message")
            println ""

            def exitOnException = options.exitOnException
            if (!exitOnException) {
                throw ignored
            }

            System.exit(1)
        }
    }

    static void setPropertyValues(OptionAccessor options) {
        def commandLine = options.getInner()
        def cliOptions = commandLine.options

        cliOptions.each {
            if ("D" == it.opt) {
                def values = it.values
                if (values.size() == 1) {
                    System.setProperty(values[0], "")
                }
                else {
                    System.setProperty(values[0], values[1])
                }
            }
        }
    }

    boolean doListJobs(OptionAccessor options) {
        def cliArgs = options.arguments()
        if ("list-jobs" == cliArgs[0]) {
            def jobDir = new File(jobPath)
            println ""

            if (jobDir.listFiles()) {
                println "Available Jobs:"
            }
            else {
                println "No jobs have been installed"
            }

            jobDir.eachFile(FileType.DIRECTORIES) {
                def m = it.name =~ /metridoc-job-(\w+)-(.+)/
                if (m.matches()) {
                    def name = m.group(1)
                    def version = m.group(2)
                    println " --> $name (v$version)"
                }
                m = it.name =~ /metridoc-job-(\w+)/
                if (m.matches()) {
                    def name = m.group(1)
                    println " --> $name"
                }
            }
            println ""

            return true
        }

        return false
    }

    @SuppressWarnings("GroovyAccessibility")
    def protected runJob(OptionAccessor options) {
        def arguments = options.arguments()
        def shortJobName = arguments[0]
        def file = new File(shortJobName)
        File metridocScript
        def loader = findHighestLevelClassLoader()
        addLibDirectories(loader)

        if (file.isFile()) {
            metridocScript = file
        }
        else if (file.isDirectory()) {
            addDirectoryResourcesToClassPath(this.class.classLoader, file)
            metridocScript = getRootScriptFromDirectory(file)
        }
        else {
            def jobDir = getJobDir(shortJobName)
            addDirectoryResourcesToClassPath(this.class.classLoader, jobDir)
            metridocScript = getRootScriptFromDirectory(jobDir, shortJobName)
        }

        def binding = new Binding()

        binding.args = [] as String[]
        //first arg is the job name
        if (arguments.size() > 1) {
            def jobArgs = arguments[1..arguments.size() - 1] as String[]
            binding.args = jobArgs
        }

        assert metridocScript && metridocScript.exists(): "root script does not exist"
        def thread = Thread.currentThread()
        setupLogging(options)
        def shell = new GroovyShell(thread.contextClassLoader, binding)
        thread.contextClassLoader = shell.classLoader
        def log = LoggerFactory.getLogger(this.getClass())
        log.info "Running $metridocScript at ${new Date()}"
        def response = null
        def throwable
        try {
            response = shell.evaluate(metridocScript)
        }
        catch (Throwable badExecution) {
            throwable = badExecution
        }
        log.info "Finished running $metridocScript at ${new Date()}"
        if (throwable) throw throwable
        return response
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected void setupLogging(OptionAccessor options) {
        def simpleLoggerClass
        try {
            simpleLoggerClass = Thread.currentThread().contextClassLoader.loadClass("org.slf4j.impl.SimpleLogger")
        }
        catch (ClassNotFoundException ignored) {
            System.err.println("Could not find SimpleLogger on the classpath, [SimpleLogger] will not be initialized")
            return
        }

        String SHOW_THREAD_NAME_KEY = simpleLoggerClass.SHOW_THREAD_NAME_KEY
        String SHOW_LOG_NAME_KEY = simpleLoggerClass.SHOW_THREAD_NAME_KEY
        String SHOW_DATE_TIME_KEY = simpleLoggerClass.SHOW_DATE_TIME_KEY

        if (options.logLevel) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", options.logLevel)
        }
        else {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error")
            System.setProperty("org.slf4j.simpleLogger.log.metridoc", "info")
        }

        System.setProperty(SHOW_DATE_TIME_KEY, "true")

        if (!options.logLineExt) {
            System.setProperty(SHOW_THREAD_NAME_KEY, "false")
            System.setProperty(SHOW_LOG_NAME_KEY, "false")
        }
    }

    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyAccessibility"])
    protected void addDirectoryResourcesToClassPath(URLClassLoader loader, File file) {
        def resourceDir = new File(file, "src/main/resources")
        if (resourceDir.exists()) {
            loader.addURL(resourceDir.toURI().toURL())
        }
        def groovyDir = new File(file, "src/main/groovy")
        if (groovyDir.exists()) {
            loader.addURL(groovyDir.toURI().toURL())
        }

        loader.addURL(file.toURI().toURL())
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected File getRootScriptFromDirectory(File directory, String shortName = null) {
        if (shortName == null) {
            def path = directory.canonicalPath
            def index = path.lastIndexOf(SystemUtils.FILE_SEPARATOR)
            shortName = getShortName(path.substring(index + 1))
        }

        def response

        response = getFileFromDirectory(directory, "metridoc.groovy")
        if (response) return response

        response = getFileFromDirectory(directory, "${shortName}.groovy")
        if (response) return response

        return response
    }

    protected static File getFileFromDirectory(File directory, String fileName) {

        def response

        response = new File(directory, fileName)
        if (response.exists()) {
            return response
        }

        def groovyDir = new File(directory, "src/main/groovy")
        if (groovyDir.exists()) {
            response = new File(groovyDir, fileName)
            if (response.exists()) {
                return response
            }
        }

        def resourcesDir = new File(directory, "src/main/resources")
        if (resourcesDir.exists()) {
            response = new File(resourcesDir, fileName)
            if (response.exists()) {
                return response
            }
        }

        return null
    }

    protected static String getShortName(String longJobName) {
        if (longJobName.startsWith(LONG_JOB_PREFIX)) {
            def shortName = longJobName.substring(LONG_JOB_PREFIX.size())
            def index = shortName.lastIndexOf("-")
            if (index != -1) {
                return shortName.substring(0, index)
            }

            return shortName
        }
        return longJobName
    }

    boolean doInstall(OptionAccessor options) {
        def cliArgs = options.arguments()

        def command = cliArgs[0]
        if (command == "install") {
            assert cliArgs.size() == 2: "when installing a job, [install] requires a location"
            installJob(cliArgs[1])
            return true
        }

        return false
    }

    protected File getJobDir(String jobName) {
        def fullJobName = jobName
        if (!fullJobName.startsWith("metridoc-job-")) {
            fullJobName = "metridoc-job-$jobName"
        }
        File jobDir = null
        new File(jobPath).eachFile(FileType.DIRECTORIES) {
            if (it.name.startsWith(fullJobName)) {
                jobDir = it
            }
        }

        if (!jobDir) {
            println ""
            println "[$jobName] is not a recognized job"
            println ""
            System.exit(3)
        }

        return jobDir
    }

    protected boolean doHelp(CliBuilder cli, OptionAccessor options) {
        def arguments = options.arguments()
        File readme
        if (arguments[0] == "help" && arguments.size() > 1) {
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

        if (askingForHelp(options)) {
            println ""
            cli.usage()
            println ""
            def mdocVersion = this.class.classLoader.getResourceAsStream("MDOC_VERSION")
            println "Currently using mdoc $mdocVersion"
            println ""
            return true
        }

        return false
    }

    protected static boolean doInstallDeps(OptionAccessor options) {
        options.arguments().contains("install-deps")
    }

    protected void checkForAndInstallDependencies(OptionAccessor options) {
        if (!dependenciesExist()) {
            new InstallMdoc(binding: new Binding(args: args)).run()
        }
        else if (doInstallDeps(options)) {
            println "Dependencies have already been installed"
        }
    }

    protected static boolean askingForHelp(OptionAccessor options) {
        !options.arguments() || options.help || options.arguments().contains("help")
    }

    protected List parseArgs() {

        def cli = new CliBuilder(
                usage: "mdoc [<command> | <job> | help | help <job>] [job options]",
                header: "\nGlobal Options:",
                footer: "\nAvailable Commands:\n" +
                        " --> list-jobs                  lists all available jobs\n" +
                        " --> install <destination>      installs a job\n" +
                        " --> help [job name]            prints README of job, or this message\n" +
                        " --> install-deps               installs dependencies if they are not there"
        )

        cli.help("prints this message")
        cli.stacktrace("prints full stacktrace on error")
        cli.D(args: 2, valueSeparator: '=', argName: 'property=value', 'sets jvm system property')
        cli.logLevel(args: 1, argName: 'level', 'sets log level (info, error, etc.)')
        cli.logLineExt("make the log line more verbose")
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
                if (it.name.endsWith(".jar")) {
                    classloader.addURL(it.toURI().toURL())
                }
            }
        }
    }

    void installJob(String urlOrPath) {
        def file = new File(urlOrPath)
        def index = urlOrPath.lastIndexOf("/")
        if (file.exists()) {
            urlOrPath = file.canonicalPath
            index = urlOrPath.lastIndexOf(SystemUtils.FILE_SEPARATOR)
        }
        def fileName = urlOrPath.substring(index + 1)
        def destinationName = fileName
        if (!fileName.startsWith(LONG_JOB_PREFIX)) {
            destinationName = "$LONG_JOB_PREFIX$fileName"
        }
        def jobPathDir = new File("$jobPath")
        if (!jobPathDir.exists()) {
            jobPathDir.mkdirs()
        }

        def m = destinationName =~ /(metridoc-job-\w+)(-v?[0-9])?/
        if (m.lookingAt()) {
            jobPathDir.eachFile(FileType.DIRECTORIES) {
                def unversionedName = m.group(1)
                if (it.name.startsWith(unversionedName)) {
                    println "deleting $it.name and installing $destinationName"
                    assert it.deleteDir(): "Could not delete $it"
                }
            }
        }

        def destination = new File(jobPathDir, destinationName)
        def fileToInstall

        try {
            fileToInstall = new URL(urlOrPath)
        }
        catch (Throwable ignored) {
            fileToInstall = new File(urlOrPath)
            if (fileToInstall.exists() && fileToInstall.isDirectory()) {
                installDirectoryJob(fileToInstall, destination)
                return
            }

            def supported = fileToInstall.exists() && fileToInstall.isFile() && fileToInstall.name.endsWith(".zip")
            if (!supported) {
                println ""
                println "$fileToInstall is not a zip file"
                println ""
                System.exit(2)
            }
        }

        fileToInstall.withInputStream { inputStream ->
            destination.newOutputStream() << inputStream
        }

        ArchiveMethods.unzip(destination, jobPathDir)
        def filesToDelete = []

        jobPathDir.eachFile {
            if (it.isFile() && it.name.endsWith(".zip")) {
                filesToDelete << it
            }
        }

        filesToDelete.each {
            it.delete()
        }
    }

    private static void installDirectoryJob(File file, File destination) {
        FileUtils.copyDirectory(file, destination)
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
