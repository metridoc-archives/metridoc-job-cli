/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



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
