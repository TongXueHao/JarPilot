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

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.ui.JBUI;
import com.jcraft.jsch.SftpProgressMonitor;
import org.htx.model.CommandTemplate;
import org.htx.service.ConnectionNotifier;
import org.htx.service.PersistentStateService;
import org.htx.service.ConnectionUtils;
import org.htx.service.SftpUtil;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Factory for creating the tool window with multiple tabs.
 *
 * @Author Hao Tong Xue
 * @Date 2025/8/20 10:00
 * @Version 1.0
 */
public class MyToolWindowFactory implements ToolWindowFactory {

    private int tabIndex = 1;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        createNewTab(project, toolWindow);
        toolWindow.addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentAdded(@NotNull ContentManagerEvent event) {
                System.out.println(1);
                ContentManagerListener.super.contentAdded(event);
            }

            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                ContentManagerListener.super.contentRemoved(event);
            }
        });

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction(null, "Tab", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                createNewTab(project, toolWindow);
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("MyToolWindowToolbar", actionGroup, true);
        toolbar.setTargetComponent(toolWindow.getComponent());
        toolWindow.setTitleActions(toolbar.getActions());
    }

    private void createNewTab(Project project, ToolWindow toolWindow) {

        String windowId = "Tab " + tabIndex;
        PersistentStateService stateService = PersistentStateService.getInstance(project);
        PersistentStateService.WindowState windowState = stateService.getWindowState(windowId);

        SpringBootLogConsole consoleView = new SpringBootLogConsole(project);

        JPanel panel = new JPanel(new BorderLayout());

        ConnectionUtils connectionUtils = new ConnectionUtils();

        ConnectionForm connectionForm = buildConnectionForm(project, windowState, connectionUtils, stateService, windowId, consoleView);
        panel.add(connectionForm.getPanel(), BorderLayout.NORTH);

        JPanel westPanel = new JPanel(new BorderLayout());

        PushForm pushForm = buildPushForm(project, connectionForm, connectionUtils, consoleView);

        ServiceForm serviceForm = buildServiceForm(project,windowState, connectionForm, connectionUtils, consoleView, stateService, windowId);

        pushForm.getPanel().setBorder(JBUI.Borders.customLineBottom(JBColor.border()));

        westPanel.add(pushForm.getPanel(), BorderLayout.NORTH);
        westPanel.add(serviceForm.getMainPanel(), BorderLayout.CENTER);
        panel.add(westPanel, BorderLayout.WEST);

        panel.add(consoleView.getConsoleView().getComponent(), BorderLayout.CENTER);

        connectionForm.setOnConnect(e->{
            JButton connectButton = connectionForm.getConnectButton();

            if (connectButton.getIcon() == AllIcons.Actions.Suspend) {
                try {
                    String port = serviceForm.getPortField().getText();
                    String jarPath = serviceForm.getJarField().getText();
                    String pidPath = jarPath.replace(".jar", ".pid");
                    JBTextField logField = serviceForm.getLogField();
                    String stopCmd = CommandTemplate.STOP_SPRING_BOOT.render(pidPath, pidPath, pidPath, port, port, port, port);
                    String closeCmd = CommandTemplate.CLOSE_SPRING_BOOT_LOG.render(logField.getText());
                    connectionUtils.exec(stopCmd);
                    connectionUtils.exec(closeCmd);

                } catch (Exception ignored) {}finally {
                    SwingUtilities.invokeLater(() -> {
                        serviceForm.getViewCloseLogButton().setIcon(AllIcons.Actions.Execute);
                        serviceForm.getStartStopButton().setIcon(AllIcons.Actions.Execute);
                        serviceForm.getStartStopButton().setEnabled(true);
                    });
                }
            }
        });

        ContentFactory contentFactory = toolWindow.getContentManager().getFactory();
        Content content = contentFactory.createContent(panel, windowId, false);
        content.setCloseable(true);
        toolWindow.getContentManager().addContent(content);

        tabIndex++;
    }

    private static @NotNull ServiceForm buildServiceForm(Project project, PersistentStateService.WindowState windowState, ConnectionForm connectionForm, ConnectionUtils connectionUtils, SpringBootLogConsole consoleView, PersistentStateService stateService, String windowId) {
        ServiceForm serviceForm = new ServiceForm(project, windowState);

        serviceForm.setOnStartStopButton(e->{

            boolean isConnected = connectionForm.getConnectButton().getIcon() == AllIcons.Actions.Suspend;
            if (!isConnected) {
                consoleView.appendLog("ERROR: Please connect first!", ConsoleViewContentType.LOG_ERROR_OUTPUT);
                ConnectionNotifier.notifyConnectionResult(project, false, "Please Connection first!");
                return;
            }

            JButton startStopButton = serviceForm.getStartStopButton();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {

                    String port = serviceForm.getPortField().getText();
                    String jarPath = serviceForm.getJarField().getText();
                    String activeText = serviceForm.getActiveField().getText();
                    String pidPath = jarPath.replace(".jar", ".pid");
                    String logPath = jarPath.replace(".jar", ".out");

                    startStopButton.setEnabled(false);

                    // ---------------- STOP ----------------
                    consoleView.appendLog("===== [STOP SERVICE ] =====", ConsoleViewContentType.NORMAL_OUTPUT);
                    String stopCmd = CommandTemplate.STOP_SPRING_BOOT.render(pidPath, pidPath, pidPath, port, port, port, port);
                    consoleView.appendLog("Command: " + stopCmd, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                    String stopOutput = connectionUtils.exec(stopCmd).trim();

                    if (stopOutput.contains("Stopped by PID file")) {
                        consoleView.appendLog(stopOutput, ConsoleViewContentType.NORMAL_OUTPUT);
                    } else if (stopOutput.contains("Stopped by port")) {
                        consoleView.appendLog(stopOutput, ConsoleViewContentType.NORMAL_OUTPUT);
                    } else if (stopOutput.contains("No service running")) {
                        consoleView.appendLog("WARNING: " + stopOutput, ConsoleViewContentType.LOG_WARNING_OUTPUT);
                    } else {
                        consoleView.appendLog("ERROR: Unexpected response: " + stopOutput, ConsoleViewContentType.LOG_ERROR_OUTPUT);
                    }

                    if (startStopButton.getIcon() == AllIcons.Actions.Suspend){
                        startStopButton.setIcon(AllIcons.Actions.Execute);
                        startStopButton.setEnabled(true);
                        return;
                    }


                    startStopButton.setIcon(AnimatedIcon.Default.INSTANCE);
                    startStopButton.repaint();

                    // ---------------- START ----------------
                    consoleView.appendLog("===== [START SERVICE] =====", ConsoleViewContentType.NORMAL_OUTPUT);
                    String startCmd = CommandTemplate.START_SPRING_BOOT_JAR_WITH_PID.render(jarPath, jarPath, StringUtil.isEmpty(activeText) ? "" : ("--spring.profiles.active=" + activeText), port, logPath, pidPath, jarPath);
                    String startOutput = connectionUtils.exec(startCmd);

                    consoleView.appendLog("Command: " + startCmd, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                    consoleView.appendLog(startOutput, ConsoleViewContentType.NORMAL_OUTPUT);

                    if (startOutput.contains("No such file") || startOutput.contains("cannot find")) {
                        throw new RuntimeException("Remote JAR file does not exist: " + jarPath);
                    } else if (startOutput.contains("Permission denied")) {
                        throw new RuntimeException("Permission denied on remote server.");
                    } else {
                        consoleView.appendLog("INFO: Service started successfully on port " + port, ConsoleViewContentType.NORMAL_OUTPUT);
                        consoleView.appendLog("INFO: Logs redirected to " + logPath, ConsoleViewContentType.LOG_WARNING_OUTPUT);

                    }

                    SwingUtilities.invokeLater(() -> {
                        serviceForm.getJarField().setEnabled(false);
                        serviceForm.getPortField().setEnabled(false);

                        startStopButton.setEnabled(true);
                        startStopButton.setIcon(AllIcons.Actions.Suspend);
                        windowState.serverPort = Integer.parseInt(port);
                        windowState.jarPath = jarPath;
                        windowState.activeField = activeText;
                        stateService.setWindowState(windowId, windowState);
                        ConnectionNotifier.notifyConnectionResult(project, true, "Start successful!");
                    });

                } catch (Exception ex) {
                    consoleView.appendLog("===== [UNEXPECTED ERROR] =====", ConsoleViewContentType.LOG_ERROR_OUTPUT);
                    consoleView.appendLog("ERROR: " + ex.getMessage(), ConsoleViewContentType.LOG_ERROR_OUTPUT);
                    SwingUtilities.invokeLater(() -> {
                        startStopButton.setEnabled(true);
                        startStopButton.setIcon(AllIcons.Actions.Execute);
                        ConnectionNotifier.notifyConnectionResult(project, false, "Start failed!");
                    });
                }
            });

        });

        serviceForm.setOnViewCloseLogButton(e->{

            JButton viewCloseLogButton = serviceForm.getViewCloseLogButton();
            JBTextField logField = serviceForm.getLogField();

            boolean isConnected = connectionForm.getConnectButton().getIcon() == AllIcons.Actions.Suspend;
            if (!isConnected) {
                ConnectionNotifier.notifyConnectionResult(project, false, "Please Connection first!");
                return;
            }

            // ---------------- CLOSE LOG STREAM ----------------
            if (viewCloseLogButton.getIcon() == AllIcons.Actions.Suspend) {
                try {
                    String closeCmd = CommandTemplate.CLOSE_SPRING_BOOT_LOG.render(logField.getText());
                    consoleView.appendLog("\n\n\n\n===== [CLOSE LOG STREAM] =====", ConsoleViewContentType.NORMAL_OUTPUT);
                    consoleView.appendLog("Command: " + closeCmd, ConsoleViewContentType.LOG_DEBUG_OUTPUT);

                    String closeOutput = connectionUtils.exec(closeCmd).trim();
                    consoleView.appendLog("Output: " + closeOutput, ConsoleViewContentType.NORMAL_OUTPUT);

                    SwingUtilities.invokeLater(() -> {
                        windowState.logPath = serviceForm.getJarField().getText().replace(".jar", ".out");
                        stateService.setWindowState(windowId, windowState);
                        viewCloseLogButton.setIcon(AllIcons.Actions.Execute);
                    });

                    consoleView.appendLog("INFO: Log streaming stopped for " + logField.getText(),
                            ConsoleViewContentType.NORMAL_OUTPUT);

                } catch (Exception ex) {
                    consoleView.appendLog("ERROR: Failed to close log stream: " + ex.getMessage(),
                            ConsoleViewContentType.LOG_ERROR_OUTPUT);
                    SwingUtilities.invokeLater(() -> viewCloseLogButton.setIcon(AllIcons.Actions.Execute));
                    throw new RuntimeException(ex);
                }

                return;

            }

            try {
                // ---------------- OPEN LOG STREAM ----------------
                consoleView.appendLog("\n\n\n\n===== [OPEN LOG STREAM] =====", ConsoleViewContentType.NORMAL_OUTPUT);
                String logCmd = CommandTemplate.START_SPRING_BOOT_LOG.render(logField.getText());
                consoleView.appendLog("Command: " + logCmd, ConsoleViewContentType.LOG_DEBUG_OUTPUT);

                connectionUtils.execStream(logCmd, consoleView::appendLog);

                consoleView.appendLog("INFO: Log streaming started from " + logField.getText(),
                        ConsoleViewContentType.NORMAL_OUTPUT);

                SwingUtilities.invokeLater(() -> viewCloseLogButton.setIcon(AllIcons.Actions.Suspend));

            } catch (Exception ex) {
                consoleView.appendLog("ERROR: Failed to open log stream: " + ex.getMessage(),
                        ConsoleViewContentType.LOG_ERROR_OUTPUT);
                throw new RuntimeException(ex);
            }

        });

        return serviceForm;
    }

    private static @NotNull ConnectionForm buildConnectionForm(Project project, PersistentStateService.WindowState windowState, ConnectionUtils connectionUtils, PersistentStateService stateService, String windowId, SpringBootLogConsole consoleView) {
        ConnectionForm connectionForm = new ConnectionForm(project, windowState);
        connectionForm.setOnConnect(e -> {
            JButton connectButton = connectionForm.getConnectButton();

            if (connectButton.getIcon() == AllIcons.Actions.Suspend) {
                try {
                    connectionUtils.close();
                    connectButton.setIcon(AllIcons.Actions.Execute);
                    connectionForm.enableAllFields();
                    consoleView.appendLog("Disconnected from " + connectionForm.getHost(), ConsoleViewContentType.NORMAL_OUTPUT);

                    ConnectionNotifier.notifyConnectionResult(project, true, "Disconnect successful!");
                } catch (Exception ex) {
                    ConnectionNotifier.notifyConnectionResult(project, false, "Disconnect failed!");
                }
                return;
            }

            connectButton.setEnabled(false);
            connectButton.setIcon(AnimatedIcon.Default.INSTANCE);

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    connectionUtils.connect(
                            connectionForm.getHost(),
                            connectionForm.getPort(),
                            connectionForm.getUser(),
                            connectionForm.getPassword()
                    );

                    SwingUtilities.invokeLater(() -> {
                        connectButton.setEnabled(true);
                        connectButton.setIcon(AllIcons.Actions.Suspend);
                        connectionForm.disableAllFields();

                        windowState.host = connectionForm.getHost();
                        windowState.user = connectionForm.getUser();
                        windowState.passWord = connectionForm.getPassword();
                        windowState.port = connectionForm.getPort();
                        stateService.setWindowState(windowId, windowState);

                        consoleView.appendLog("Connected to " + connectionForm.getHost() + " successfully.", ConsoleViewContentType.NORMAL_OUTPUT);
                        ConnectionNotifier.notifyConnectionResult(project, true, "Connection successful!");
                    });

                } catch (IOException ex1) {
                    SwingUtilities.invokeLater(() -> {
                        connectButton.setEnabled(true);
                        connectButton.setIcon(AllIcons.Actions.Execute);
                    });
                    consoleView.appendLog("ERROR: Connection failed: " + ex1.getMessage(), ConsoleViewContentType.LOG_ERROR_OUTPUT);
                    ConnectionNotifier.notifyConnectionResult(project, false, "Connection failed!");
                }
            });
        });
        return connectionForm;
    }

    private static @NotNull PushForm buildPushForm(Project project, ConnectionForm connectionForm, ConnectionUtils connectionUtils, SpringBootLogConsole consoleView) {
        PushForm pushForm = new PushForm(project);
        pushForm.setOnUpload(e -> {

            boolean isConnected = connectionForm.getConnectButton().getIcon() == AllIcons.Actions.Suspend;
            if (!isConnected) {
                consoleView.appendLog("ERROR: Please connect first!", ConsoleViewContentType.LOG_ERROR_OUTPUT);
                ConnectionNotifier.notifyConnectionResult(project, false, "Please Connection first!");
                return;
            }

            String localPath = pushForm.getLocalJarPath();
            String remotePath = pushForm.getRemotePath();

            File localFile = new File(localPath);
            long localFileLength = localFile.length();

            pushForm.updateUploadButton(false);
            JProgressBar progressBar = pushForm.getProgressBar();

            ApplicationManager.getApplication().executeOnPooledThread(() -> {

                File remoteFile = new File(remotePath);

                try {
                    try {
                        connectionUtils.exec(CommandTemplate.MKDIRS_UNIX.render(remoteFile.getParent()));
                    }catch (Exception ignored){}
                    connectionUtils.exec(CommandTemplate.DELETE_FILE_UNIX.render(remotePath));
                    SftpUtil.upload(
                            connectionForm.getHost(), connectionForm.getPort(), connectionForm.getUser(), connectionForm.getPassword(),
                            localPath,
                            remotePath,
                            new SftpProgressMonitor() {
                                long transferred = 0;
                                final long startTime = System.currentTimeMillis();
                                int lastLoggedProgress = 0;

                                @Override
                                public void init(int op, String src, String dest, long max) {
                                    progressBar.setValue(0);
                                    String logMsg = String.format("|%-20s|   0%% (start upload %s -> %s)",
                                            "", localPath, dest);
                                    consoleView.appendLog(logMsg, ConsoleViewContentType.NORMAL_OUTPUT);
                                }

                                @Override
                                public boolean count(long count) {
                                    transferred += count;
                                    double progress = (double) transferred / localFileLength * 100;
                                    long elapsed = System.currentTimeMillis() - startTime;
                                    double speed = (elapsed > 0) ? transferred / 1024.0 / (elapsed / 1000.0) : 0;
                                    String speedText;
                                    int currentProgress = (int) progress;

                                    if (speed >= 1024) {
                                        speedText = String.format("%.2f MB/s", speed / 1024.0);
                                    } else {
                                        speedText = String.format("%.2f KB/s", speed);
                                    }

                                    if (currentProgress >= lastLoggedProgress + 5 || currentProgress == 100) {
                                        int barLength = 20;
                                        int filled = (int) (progress / 100 * barLength);
                                        String bar = "|" + "=".repeat(filled) + " ".repeat(barLength - filled) + "|";
                                        String logMsg = String.format("%s %3d%% (%s)", bar, currentProgress, speedText);

                                        consoleView.appendLog(logMsg, ConsoleViewContentType.NORMAL_OUTPUT);
                                        lastLoggedProgress = currentProgress;
                                    }

                                    SwingUtilities.invokeLater(() -> {
                                        progressBar.setValue((int) progress);
                                        pushForm.getUploadInfo().setText(String.format("%.0f%%   %s", progress, speedText));
                                    });
                                    return true;
                                }

                                @Override
                                public void end() {
                                    consoleView.appendLog( String.format("|%-20s|   100%% (upload complete %s -> %s)","", localPath, remotePath), ConsoleViewContentType.NORMAL_OUTPUT);
                                    ConnectionNotifier.notifyConnectionResult(project, true, remoteFile.getName() + " Upload successful!");
                                    pushForm.getUploadInfo().setText("100%");
                                }
                            }
                    );
                } catch (Exception ex) {
                    ConnectionNotifier.notifyConnectionResult(project, false, remoteFile.getName() + " Upload fail!");
                    throw new RuntimeException(ex);
                }finally {
                    pushForm.updateUploadButton(true);
                }
            });

        });
        return pushForm;
    }

}
