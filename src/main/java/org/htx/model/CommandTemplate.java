/**
 * MIT License
 *
 * Copyright (c) 2025 Hao Tong Xue
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.htx.model;

/**
 * command template
 *
 * @Author Hao Tong Xue
 * @Date 2025/8/27 14:32
 * @Version 1.0
 */
public class CommandTemplate {

    private final String template;

    public CommandTemplate(String template) {
        this.template = template;
    }

    /**
     * build command
     * use String.format() replace %s
     */
    public String render(Object... args) {
        return String.format(template, args);
    }

    // ===== command template =====

    /** Linux / macOS del file */
    public static final CommandTemplate DELETE_FILE_UNIX =
            new CommandTemplate("rm -f \"%s\"");

    /** Linux mkdir folders */
    public static final CommandTemplate MKDIRS_UNIX =
            new CommandTemplate("mkdir -p \"%s\"");

    /** Linux start spring boot jar */
    public static final CommandTemplate START_SPRING_BOOT_JAR_WITH_PID =
            new CommandTemplate(
                    "if [ -f %s ]; then " +
                            "nohup java -jar %s %s --server.port=%s > %s 2>&1 & echo $! > %s; " +
                            "else echo 'No such file: %s'; fi"
            );

    /** Linux Start */
    public static final CommandTemplate START_SPRING_BOOT_LOG =
            new CommandTemplate("tail -f %s");

    /** Linux stop spring boot jar */
    public static final CommandTemplate CLOSE_SPRING_BOOT_LOG =
            new CommandTemplate("pkill -f 'tail -f %s'");


    public static final CommandTemplate STOP_SPRING_BOOT =
            new CommandTemplate(
                    // Step 1: PID file
                    "if [ -f %s ]; then " +
                            "pid=$(cat %s); " +
                            "if kill -9 $pid 2>/dev/null; then " +
                            "echo 'Stopped by PID file (PID: '$pid')'; " +
                            "else " +
                            "echo 'PID file exists but process not running'; " +
                            "fi; " +
                            "rm -f %s; " +
                            "else " +
                            "echo 'No PID file found, trying port...'; " +
                            "fi; " +

                            // Step 2: port
                            "pid=$(lsof -ti:%s 2>/dev/null || netstat -nlp 2>/dev/null | grep ':%s' | awk '{print $7}' | cut -d'/' -f1); " +
                            "if [ -n \"$pid\" ]; then " +
                            "kill -9 $pid && echo 'Stopped by port %s (PID: '$pid')'; " +
                            "else " +
                            "echo 'No service running on port %s'; " +
                            "fi"
            );




}
