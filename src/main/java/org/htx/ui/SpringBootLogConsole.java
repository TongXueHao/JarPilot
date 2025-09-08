package org.htx.ui;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import org.htx.log.LogColorDetector;

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
