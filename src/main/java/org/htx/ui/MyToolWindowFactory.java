package org.htx.ui;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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

        ConnectionForm connectionForm = buildConnectionForm(project, windowState, connectionUtils, stateService, windowId);
        panel.add(connectionForm.getPanel(), BorderLayout.NORTH);

        JPanel westPanel = new JPanel(new BorderLayout());

        PushForm pushForm = buildPushForm(project, connectionForm, connectionUtils);

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
                ConnectionNotifier.notifyConnectionResult(project, false, "Please Connection first!");
                return;
            }

            JButton startStopButton = serviceForm.getStartStopButton();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {

                    String port = serviceForm.getPortField().getText();
                    String jarPath = serviceForm.getJarField().getText();
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
                    String startCmd = CommandTemplate.START_SPRING_BOOT_JAR_WITH_PID.render(jarPath, jarPath, port, logPath, pidPath, jarPath);
                    String startOutput = connectionUtils.exec(startCmd);

                    consoleView.appendLog("Command: " + startCmd, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                    consoleView.appendLog(startOutput, ConsoleViewContentType.NORMAL_OUTPUT);

                    if (startOutput.contains("No such file") || startOutput.contains("cannot find")) {
                        consoleView.appendLog("ERROR: Remote JAR file does not exist: " + jarPath, ConsoleViewContentType.ERROR_OUTPUT);
                        return;
                    } else if (startOutput.contains("Permission denied")) {
                        consoleView.appendLog("ERROR: Permission denied on remote server.", ConsoleViewContentType.ERROR_OUTPUT);
                        return;
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

    private static @NotNull ConnectionForm buildConnectionForm(Project project, PersistentStateService.WindowState windowState, ConnectionUtils connectionUtils, PersistentStateService stateService, String windowId) {
        ConnectionForm connectionForm = new ConnectionForm(project, windowState);
        connectionForm.setOnConnect(e -> {
            JButton connectButton = connectionForm.getConnectButton();

            if (connectButton.getIcon() == AllIcons.Actions.Suspend) {
                try {
                    connectionUtils.close();
                    connectButton.setIcon(AllIcons.Actions.Execute);
                    connectionForm.enableAllFields();
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

                        ConnectionNotifier.notifyConnectionResult(project, true, "Connection successful!");
                    });

                } catch (IOException ex1) {
                    SwingUtilities.invokeLater(() -> {
                        connectButton.setEnabled(true);
                        connectButton.setIcon(AllIcons.Actions.Execute);
                    });
                    ConnectionNotifier.notifyConnectionResult(project, false, "Connection failed!");
                }
            });
        });
        return connectionForm;
    }

    private static @NotNull PushForm buildPushForm(Project project, ConnectionForm connectionForm, ConnectionUtils connectionUtils) {
        PushForm pushForm = new PushForm(project);
        pushForm.setOnUpload(e -> {

            boolean isConnected = connectionForm.getConnectButton().getIcon() == AllIcons.Actions.Suspend;
            if (!isConnected) {
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
                    connectionUtils.exec(CommandTemplate.MKDIRS_UNIX.render(remoteFile.getParent()));
                    connectionUtils.exec(CommandTemplate.DELETE_FILE_UNIX.render(remotePath));
                    SftpUtil.upload(
                            connectionForm.getHost(), connectionForm.getPort(), connectionForm.getUser(), connectionForm.getPassword(),
                            localPath,
                            remotePath,
                            new SftpProgressMonitor() {
                                long transferred = 0;
                                final long startTime = System.currentTimeMillis();

                                @Override
                                public void init(int op, String src, String dest, long max) {
                                    progressBar.setValue(0);
                                }

                                @Override
                                public boolean count(long count) {
                                    transferred += count;
                                    double progress = (double) transferred / localFileLength * 100;
                                    long elapsed = System.currentTimeMillis() - startTime;
                                    double speed = (elapsed > 0) ? transferred / 1024.0 / (elapsed / 1000.0) : 0;
                                    String speedText;

                                    if (speed >= 1024) {
                                        speedText = String.format("%.2f MB/s", speed / 1024.0);
                                    } else {
                                        speedText = String.format("%.2f KB/s", speed);
                                    }

                                    SwingUtilities.invokeLater(() -> {
                                        progressBar.setValue((int) progress);
                                        pushForm.getUploadInfo().setText(String.format("%.0f%%   %s", progress, speedText));
                                    });
                                    return true;
                                }

                                @Override
                                public void end() {
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
