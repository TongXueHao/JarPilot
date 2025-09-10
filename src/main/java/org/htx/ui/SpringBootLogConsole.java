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
package org.htx.ui;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import org.htx.log.LogColorDetector;

/**
 * Console for displaying Spring Boot logs with color coding.
 *
 * @Author Hao Tong Xue
 * @Date 2025/8/20 10:30
 * @Version 1.0
 */
public class SpringBootLogConsole {
    private final ConsoleView consoleView;

    public SpringBootLogConsole(Project project) {
        this.consoleView = new ConsoleViewImpl(project, true);
    }

    public ConsoleView getConsoleView() {
        return consoleView;
    }

    public void appendLog(String line) {
        ConsoleViewContentType type = LogColorDetector.detectType(line);
        consoleView.print(line + "\n", type);
    }

    public void appendLog(String line, ConsoleViewContentType type) {
        consoleView.print(line + "\n", type);
    }

}
