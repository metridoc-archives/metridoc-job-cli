package metridoc.cli

import groovy.sql.Sql
import metridoc.utils.ArchiveMethods

import java.util.concurrent.CountDownLatch


class InstallMdocDependencies {

    static void downloadDependencies (String[] args) {
        File destination = getDestination()
        Set<String> currentLibs = getCurrentLibs(destination)

        def slash = System.getProperty("file.separator")
        def metridocVersion = this.getClass().classLoader.getResourceAsStream("MDOC_VERSION").getText("utf-8")
        def home = System.getProperty("user.home")
        def mdocHome = "${home}${slash}.metridoc"
        def os = System.getProperty("os.name")
        boolean windows = os.contains("indows") //ignore case

        String sourceDirectory
        def userDir = System.getProperty("user.dir")

        sourceDirectory = getSourceDirectory(args, userDir, mdocHome, slash, metridocVersion)

        doGrabDependencies(windows, sourceDirectory, slash, destination, metridocVersion)

        addDependenciesToClassPath(destination, currentLibs)
    }

    protected static void doGrabDependencies(boolean windows, String sourceDirectory, slash, File destination, metridocVersion) {
        def commandPrefix = []
        if (windows) {
            commandPrefix.addAll(["cmd.exe", "/C"])
        }

        String gradle = windows ? "$sourceDirectory${slash}gradlew.bat" : "$sourceDirectory${slash}gradlew"

        def command = []
        command.addAll(commandPrefix)
        command.addAll([gradle, "-PdependenciesDestination=$destination" as String, "copyDependenciesToLib"])

        print "Downloading Dependencies"

        def latch = new CountDownLatch(1)

        def exit = 0
        Process process = null

        Thread.start {
            try {
                def sourceDirectoryDir = new File(sourceDirectory)
                if (!sourceDirectoryDir.exists() || !new File(sourceDirectoryDir, "gradlew").exists()) {
                    sourceDirectoryDir.parentFile.mkdirs()
                    String path = "https://github.com/metridoc/metridoc-job-cli/archive/v${metridocVersion}.zip"
                    new URL(path).withInputStream { inputStream ->
                        def zipFile = new File(sourceDirectoryDir.parentFile, "metridoc-job-cli-${metridocVersion}.zip" as String)
                        zipFile.withOutputStream { outputStream ->
                            outputStream << inputStream
                        }
                        ArchiveMethods.unzip(zipFile, sourceDirectoryDir.parentFile)
                    }
                }
                process = new ProcessBuilder(command as List<String>)
                        .redirectErrorStream(true)
                        .directory(sourceDirectoryDir)
                        .start()

                exit = process.waitFor()
            }
            catch (Throwable throwable) {
                println ""
                throwable.printStackTrace()
                System.exit(1)
            }

            latch.countDown()
        }

        while (latch.count != 0) {
            print "."
            Thread.sleep(200)
        }

        println "" //print blank space

        if (exit != 0) {
            println process.in.text
            System.exit(1)
        }
    }

    protected static String getSourceDirectory(args, userDir, mdocHome, slash, metridocVersion) {
        String sourceDirectory
        def buildFile = new File("${userDir}/build.gradle")
        def buildFileIsCli = false

        if (buildFile.exists()) {
            buildFileIsCli = buildFile.text.contains("metridoc-job-cli")
        }

        if (buildFileIsCli) {
            sourceDirectory = userDir
        }
        else {
            sourceDirectory = "${mdocHome}${slash}cli${slash}source${slash}metridoc-job-cli-${metridocVersion}"
        }

        if (args) {
            args.each {
                def m = it =~ /^-?-sourceDirectory=(.+)$/
                if(m.matches()) {
                    sourceDirectory = m.group(1)
                }
            }
        }

        println "using application at $sourceDirectory to install dependencies"

        sourceDirectory
    }

    protected static Set<String> getCurrentLibs(File destination) {
        Set currentLibs = [] as Set<String>
        destination.eachFile {
            currentLibs << it.name
        }
        currentLibs
    }

    protected static File getDestination() {
        def classpath = System.getProperty("java.class.path")
        def destination = new File(classpath.split(":")[0]).parentFile
        destination
    }

    protected static void addDependenciesToClassPath(File destination, currentLibs) {
        def classLoader = Sql.classLoader
        while (!(classLoader instanceof URLClassLoader)) {
            classLoader = classLoader.parent
        }

        destination.eachFile {
            if (!currentLibs.contains(it.name)) {
                classLoader.addURL(it.toURI().toURL())
            }
        }
    }
}

