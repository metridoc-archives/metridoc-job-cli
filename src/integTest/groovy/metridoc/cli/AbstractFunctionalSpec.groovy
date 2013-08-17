package metridoc.cli

import spock.lang.Specification

/**
 * @author Tommy Barker
 */
class AbstractFunctionalSpec extends Specification {

    // It seems this needs to be protected for some reason, otherwise the tests
    // throw a MissingPropertyException.
    protected final Object _outputLock = new Object()

    protected processOutput = new StringBuilder()

    protected final baseWorkDir = System.getProperty("user.dir")

    protected final env = [:]

    int runCommand(List cmdList, List inputs = []) {
        resetOutput()

        def mdocInstallDir = System.getProperty("user.dir") + "/build/install/mdoc"
        mdocInstallDir = new File(mdocInstallDir)

        // The PATH environment is needed to find the `java` command.
        if (!env["PATH"]) {
            env["PATH"] = System.getenv("PATH")
        }

        // The execute() method expects the environment as a list of strings of
        // the form VAR=value.
        def envp = env.collect { key, value -> key + "=" + value }

        Process process = (["${mdocInstallDir}/bin/mdoc", "--stacktrace"] + cmdList).execute(envp,
                new File(baseWorkDir))

        if (inputs) {
            def newLine = System.getProperty("line.separator")
            def line = new StringBuilder()
            inputs.each { String item ->
                line << item << newLine
            }

            // We're deliberately using the platform encoding when converting
            // the string to bytes, since that's the encoding the terminal is
            // likely using when users manually enter text in answer to ask()
            // questions.
            process.outputStream.write(line.toString().bytes)
            process.outputStream.flush()
        }

        def stdoutThread = consumeProcessStream(process.inputStream)
        def stderrThread = consumeProcessStream(process.errorStream)
        process.waitFor()
        int exitCode = process.exitValue()

        // The process may finish before the consuming threads have finished, so
        // given them a chance to complete so that we have the command output in
        // the buffer.
        stdoutThread.join 1000
        stderrThread.join 1000
        println "Output from executing ${cmdList.join(' ')}"
        println "---------------------"
        println output
        return exitCode
    }

    /**
     * Returns the text output (both stdout and stderr) of the last command
     * that was executed.
     */
    String getOutput() {
        return processOutput.toString()
    }

    /**
     * Clears the saved command output.
     */
    void resetOutput() {
        synchronized (this._outputLock) {
            processOutput = new StringBuilder()
        }
    }

    private Thread consumeProcessStream(final InputStream stream) {
        char[] buffer = new char[256]
        Thread.start {
            def reader = new InputStreamReader(stream)
            def charsRead = 0
            while (charsRead != -1) {
                charsRead = reader.read(buffer, 0, 256)
                if (charsRead > 0) {
                    synchronized (this._outputLock) {
                        processOutput.append(buffer, 0, charsRead)
                    }
                }
            }
        }
    }

    private void removeFromOutput(String line) {
        synchronized (this._outputLock) {
            def pos = processOutput.indexOf(line)
            if (pos != -1) {
                processOutput.delete(pos, pos + line.size() - 1)
            }
        }
    }

}
