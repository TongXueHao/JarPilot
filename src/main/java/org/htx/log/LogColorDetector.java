package org.htx.log;

import com.intellij.execution.ui.ConsoleViewContentType;

import java.util.regex.Pattern;

public class LogColorDetector {

    private static final Pattern LEVEL_PATTERN =
            Pattern.compile("\\b(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\b");

    private static final Pattern EX_START =
            Pattern.compile("^(?:[a-zA-Z0-9_.]+\\.)+[A-Z][A-Za-z0-9_$]+(?:Exception|Error)(:.*)?$");
    private static final Pattern STACK_LINE =
            Pattern.compile("^\\s*at\\s+.*\\(.*\\)$");
    private static final Pattern CAUSED_BY =
            Pattern.compile("^\\s*Caused by:.*$");
    private static final Pattern SUPPRESSED =
            Pattern.compile("^\\s*Suppressed:.*$");
    private static final Pattern ELLIPSIS =
            Pattern.compile("^\\s*\\.\\.\\.\\s+\\d+\\s+more$");

    private static boolean inThrowable = false;

    public static ConsoleViewContentType detectType(String line) {
        if (line == null || line.isEmpty()) {
            inThrowable = false;
            return ConsoleViewContentType.LOG_INFO_OUTPUT;
        }

        if (LEVEL_PATTERN.matcher(line.toUpperCase()).find()) {
            inThrowable = false;
            if (line.contains("ERROR") || line.contains("FATAL")) {
                return ConsoleViewContentType.LOG_ERROR_OUTPUT;
            } else if (line.contains("WARN")) {
                return ConsoleViewContentType.LOG_WARNING_OUTPUT;
            } else if (line.contains("DEBUG") || line.contains("TRACE")) {
                return ConsoleViewContentType.LOG_DEBUG_OUTPUT;
            } else {
                return ConsoleViewContentType.LOG_INFO_OUTPUT;
            }
        }

        if (EX_START.matcher(line).find() || CAUSED_BY.matcher(line).find()) {
            inThrowable = true;
            return ConsoleViewContentType.LOG_ERROR_OUTPUT;
        }

        if (inThrowable &&
                (STACK_LINE.matcher(line).find()
                        || SUPPRESSED.matcher(line).find()
                        || ELLIPSIS.matcher(line).find())) {
            return ConsoleViewContentType.LOG_ERROR_OUTPUT;
        }

        if (inThrowable) {
            return ConsoleViewContentType.LOG_ERROR_OUTPUT;
        }

        return ConsoleViewContentType.LOG_INFO_OUTPUT;
    }
}
