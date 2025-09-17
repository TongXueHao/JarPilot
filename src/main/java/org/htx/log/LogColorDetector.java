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
package org.htx.log;

import com.intellij.execution.ui.ConsoleViewContentType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogColorDetector {

    // 捕获日志级别，大小写不敏感，支持 WARN / WARNING
    private static final Pattern LEVEL_PATTERN =
            Pattern.compile("\\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL)\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern EX_START =
            Pattern.compile("^(?:[a-zA-Z0-9_.]+\\.)?[A-Z][A-Za-z0-9_$]+(?:Exception|Error)(:.*)?$");
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

        Matcher levelMatcher = LEVEL_PATTERN.matcher(line);
        if (levelMatcher.find()) {
            inThrowable = false;
            String level = levelMatcher.group(1).toUpperCase();
            switch (level) {
                case "ERROR":
                case "FATAL":
                    return ConsoleViewContentType.LOG_ERROR_OUTPUT;
                case "WARN":
                case "WARNING":
                    return ConsoleViewContentType.LOG_INFO_OUTPUT;
                case "DEBUG":
                case "TRACE":
                    return ConsoleViewContentType.LOG_DEBUG_OUTPUT;
                case "INFO":
                default:
                    return ConsoleViewContentType.LOG_WARNING_OUTPUT;
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