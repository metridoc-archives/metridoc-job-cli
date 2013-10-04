package metridoc.utils
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

/**
 * @author Tommy Barker
 */
class JansiPrintWriter extends PrintStream {

    JansiPrintWriter(OutputStream out) {
        super(out, true)
    }

    @Override
    void print(String text) {
        if(!text.trim()) {
            super.print("")
            return
        }

        super.print(ansi().fg(getColor(text)).a(text).reset())
    }

    @Override
    void println(String text) {
        if(!text.trim()) {
            super.println("")
            return
        }

        super.println(ansi().fg(getColor(text)).a(text).reset())
    }

    Color getColor(String text) {
        if(text.contains("WARN")) {
            return YELLOW
        }

        if(text.contains("ERROR")) {
            return RED
        }

        if(text.contains("Exception:")) {
            return RED
        }

        if(text.trim().startsWith("at ") && !text.startsWith("at ")) {
            return RED
        }

        return GREEN
    }
}
