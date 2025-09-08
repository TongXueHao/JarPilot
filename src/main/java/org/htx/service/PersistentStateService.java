package org.htx.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent state for multiple tool windows
 */
@State(
        name = "MultiWindowStateService",
        storages = @Storage("MultiWindowState.xml")
)
public class PersistentStateService implements PersistentStateComponent<PersistentStateService.State> {

    public static class WindowState {
        public String host = "";
        public String user = "";
        public String passWord = "";
        public int port = 22;
        public int serverPort = 8080;
        public String jarPath = "";
        public String logPath = "";
    }

    public static class State {
        // key = windowId, value = window state
        public Map<String, WindowState> windowStates = new HashMap<>();
    }

    private State state = new State();

    public static PersistentStateService getInstance(@NotNull Project project) {
        return project.getService(PersistentStateService.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    /** Get or create state for a specific window */
    public WindowState getWindowState(String windowId) {
        return state.windowStates.computeIfAbsent(windowId, id -> new WindowState());
    }

    /** Update state for a specific window */
    public void setWindowState(String windowId, WindowState windowState) {
        state.windowStates.put(windowId, windowState);
    }
}
