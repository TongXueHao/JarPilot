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
package org.htx.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
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
        public String activeField = "";
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
        WindowState windowState = state.windowStates.computeIfAbsent(windowId, id -> new WindowState());
        if (StringUtils.isNoneEmpty(windowState.passWord)) {
            windowState.passWord = CryptoUtils.decrypt(windowState.passWord);
        }
        return windowState;
    }

    /** Update state for a specific window */
    public void setWindowState(String windowId, WindowState windowState) {
        if(StringUtils.isNoneEmpty(windowState.passWord)){
            windowState.passWord = CryptoUtils.encrypt(windowState.passWord);
        }
        state.windowStates.put(windowId, windowState);
    }

    public Map<String, WindowState> getAllWindowStatesMutable() {
        return state.windowStates;
    }

}
