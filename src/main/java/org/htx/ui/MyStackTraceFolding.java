package org.htx.ui;

import com.intellij.execution.ConsoleFolding;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MyStackTraceFolding extends ConsoleFolding {
    @Override
    public boolean shouldFoldLine(@NotNull Project project, @NotNull String line) {
        return line.trim().startsWith("at java.")
            || line.trim().startsWith("at javax.")
            || line.trim().startsWith("at sun.")
            || line.trim().startsWith("at org.springframework."); 
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull Project project, @NotNull List<String> lines) {
        return "... " + lines.size() + " lines folded ...";
    }
}
