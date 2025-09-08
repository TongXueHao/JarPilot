package org.htx.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import org.htx.service.PersistentStateService;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;

public class ConnectionForm {

    private final JBTextField hostField = new JBTextField(10);
    private final JBTextField userField = new JBTextField(10);
    private final JBPasswordField passwordField = new JBPasswordField();
    private final JBTextField portField = new JBTextField("22", 10);

    private final JButton connectButton = new JButton();
    private final JBPanel mainPanel;

    private static final String IPV4_REGEX =
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}" +
                    "(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$";

    public ConnectionForm(Project project, PersistentStateService.WindowState windowState) {

        hostField.getEmptyText().setText("Host");
        userField.getEmptyText().setText("User");
        passwordField.getEmptyText().setText("Password");
        portField.getEmptyText().setText("Port");

        hostField.setText(windowState.host);
        userField.setText(windowState.user);
        passwordField.setText(windowState.passWord);
        portField.setText(String.valueOf(windowState.port));

        passwordField.setColumns(20);
        connectButton.setIcon(AllIcons.Actions.Execute);
        int size = 30;
        connectButton.setPreferredSize(new Dimension(size, size));
        connectButton.setMinimumSize(new Dimension(size, size));
        connectButton.setMaximumSize(new Dimension(size, size));

        mainPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT));
        mainPanel.setBorder(JBUI.Borders.customLineBottom(JBColor.border()));

        mainPanel.add(new JLabel("Host:"));
        mainPanel.add(hostField);
        mainPanel.add(new JLabel("User:"));
        mainPanel.add(userField);
        mainPanel.add(new JLabel("Password:"));
        mainPanel.add(passwordField);
        mainPanel.add(new JLabel("Port:"));
        mainPanel.add(portField);
        mainPanel.add(connectButton);

        setupValidation(project);

        connectButton.addActionListener(e -> validateAllFields());

    }

    private void setupValidation(Project project) {

        DocumentAdapter adapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull @org.jetbrains.annotations.NotNull DocumentEvent e) {
                validateAllFields();
            }
        };

        hostField.getDocument().addDocumentListener(adapter);
        portField.getDocument().addDocumentListener(adapter);
        userField.getDocument().addDocumentListener(adapter);
        passwordField.getDocument().addDocumentListener(adapter);

        new ComponentValidator(project)
                .withValidator(() -> {
                    String host = hostField.getText();
                    if (StringUtil.isEmpty(host)) {
                        return new ValidationInfo("Host cannot be empty", hostField);
                    }
                    if (!host.matches(IPV4_REGEX)) {
                        return new ValidationInfo("Host must be a valid IPv4 address", hostField);
                    }
                    return null;
                }).installOn(hostField);
        hostField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                ComponentValidator.getInstance(hostField)
                        .ifPresent(ComponentValidator::revalidate);
            }
        });

        new ComponentValidator(project)
                .withValidator(() -> {
                    String pt = portField.getText();
                    if (StringUtil.isEmpty(pt)) {
                        return new ValidationInfo("Port cannot be empty", portField);
                    }
                    try {
                        int portValue = Integer.parseInt(pt);
                        if (portValue < 0 || portValue > 65535) {
                            return new ValidationInfo("Port must be 0~65535", portField);
                        }
                    } catch (NumberFormatException e) {
                        return new ValidationInfo("Port must be a number", portField);
                    }
                    return null;
                }).installOn(portField);
        portField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull DocumentEvent e) {
                ComponentValidator.getInstance(portField)
                        .ifPresent(ComponentValidator::revalidate);
            }
        });

        new ComponentValidator(project)
                .withValidator(() -> {
                    if (StringUtil.isEmpty(userField.getText())) {
                        return new ValidationInfo("User cannot be empty", userField);
                    }
                    return null;
                }).installOn(userField);

        userField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull DocumentEvent e) {
                ComponentValidator.getInstance(userField)
                        .ifPresent(ComponentValidator::revalidate);
            }
        });

        new ComponentValidator(project)
                .withValidator(() -> {
                    if (StringUtil.isEmpty(new String(passwordField.getPassword()))) {
                        return new ValidationInfo("Password cannot be empty", passwordField);
                    }
                    return null;
                }).installOn(passwordField);

        passwordField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull DocumentEvent e) {
                ComponentValidator.getInstance(passwordField)
                        .ifPresent(ComponentValidator::revalidate);
            }
        });
    }

    private void validateAllFields() {
        boolean valid = true;

        String host = hostField.getText();
        if (StringUtil.isEmpty(host) || !host.matches(IPV4_REGEX)) {
            valid = false;
        }

        String portText = portField.getText();
        try {
            int port = Integer.parseInt(portText);
            if (port < 0 || port > 65535) valid = false;
        } catch (NumberFormatException e) {
            valid = false;
        }

        if (StringUtil.isEmpty(userField.getText())) valid = false;

        if (StringUtil.isEmpty(new String(passwordField.getPassword()))) valid = false;

        connectButton.setEnabled(valid);
    }

    public void disableAllFields() {
        hostField.setEditable(false);
        hostField.setEnabled(false);

        userField.setEditable(false);
        userField.setEnabled(false);

        passwordField.setEditable(false);
        passwordField.setEnabled(false);

        portField.setEditable(false);
        portField.setEnabled(false);
    }

    public void enableAllFields() {
        hostField.setEditable(true);
        hostField.setEnabled(true);

        userField.setEditable(true);
        userField.setEnabled(true);

        passwordField.setEditable(true);
        passwordField.setEnabled(true);

        portField.setEditable(true);
        portField.setEnabled(true);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void setOnConnect(ActionListener listener) {
        connectButton.addActionListener(listener);
    }

    public String getHost() {
        return hostField.getText();
    }

    public String getUser() {
        return userField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public Integer getPort() {
        return Integer.parseInt(portField.getText());
    }

    public JButton getConnectButton() {
        return connectButton;
    }

}
