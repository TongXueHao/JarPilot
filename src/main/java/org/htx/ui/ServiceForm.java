package org.htx.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.htx.service.PersistentStateService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;

public class ServiceForm {
    private final JPanel mainPanel;
    private final JBTextField jarField;
    private final JBTextField portField;
    private final JBTextField logField;
    private final JButton startStopButton = new JButton();
    private final JButton viewCloseLogButton = new JButton();

    public ServiceForm(@NotNull Project project, PersistentStateService.WindowState windowState) {
        jarField = new JBTextField(20);
        portField = new JBTextField(20);
        logField = new JBTextField(20);

        jarField.setText(windowState.jarPath);
        portField.setText(windowState.serverPort + "");
        logField.setText(windowState.logPath);

        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        mainPanel.add(new JLabel("JAR Address:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(jarField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        mainPanel.add(Box.createHorizontalStrut(30), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(portField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;

        startStopButton.setIcon(AllIcons.Actions.Execute);
        int size = 30;
        startStopButton.setPreferredSize(new Dimension(size, size));
        startStopButton.setMinimumSize(new Dimension(size, size));
        startStopButton.setMaximumSize(new Dimension(size, size));
        mainPanel.add(startStopButton, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Log Address:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(logField, gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.weightx = 0;

        viewCloseLogButton.setIcon(AllIcons.Actions.Execute);
        viewCloseLogButton.setPreferredSize(new Dimension(size, size));
        viewCloseLogButton.setMinimumSize(new Dimension(size, size));
        viewCloseLogButton.setMaximumSize(new Dimension(size, size));
        mainPanel.add(viewCloseLogButton, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        setupValidation(project);
        validateStartStopButton();

        viewCloseLogButton.setEnabled(!logField.getText().isEmpty());

    }

    private void setupValidation(Project project) {

        DocumentAdapter adapter = new DocumentAdapter() {
            @Override
            protected void textChanged(@groovyjarjarantlr4.v4.runtime.misc.NotNull @org.jetbrains.annotations.NotNull DocumentEvent e) {
                validateStartStopButton();
            }
        };

        jarField.getDocument().addDocumentListener(adapter);
        portField.getDocument().addDocumentListener(adapter);

        new ComponentValidator(project)
                .withValidator(() -> {
                    if (StringUtil.isEmpty(jarField.getText())) {
                        return new ValidationInfo("JarField cannot be empty", jarField);
                    }

                    if (!jarField.getText().endsWith(".jar")) {
                        return new ValidationInfo("JarField must end with .jar", jarField);
                    }
                    return null;
                }).installOn(jarField);

        jarField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull DocumentEvent e) {
                ComponentValidator.getInstance(jarField)
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
                    if (StringUtil.isNotEmpty(logField.getText())) {
                        viewCloseLogButton.setEnabled(true);
                        return new ValidationInfo("LogField cannot be empty", logField);
                    }
                    viewCloseLogButton.setEnabled(false);
                    return null;
                }).installOn(logField);
        logField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull DocumentEvent e) {
                ComponentValidator.getInstance(logField)
                        .ifPresent(ComponentValidator::revalidate);
            }
        });

    }

    private void validateStartStopButton() {
        boolean valid = true;
        String portText = portField.getText();
        try {
            int port = Integer.parseInt(portText);
            if (port < 0 || port > 65535) valid = false;
        } catch (NumberFormatException e) {
            valid = false;
        }

        if (StringUtil.isEmpty(jarField.getText())) valid = false;

        startStopButton.setEnabled(valid);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JButton getStartStopButton() {
        return startStopButton;
    }

    public JButton getViewCloseLogButton() {
        return viewCloseLogButton;
    }

    public JBTextField getJarField() {
        return jarField;
    }

    public JBTextField getPortField() {
        return portField;
    }

    public JBTextField getLogField() {
        return logField;
    }

    public void setOnStartStopButton(ActionListener listener) {
        startStopButton.addActionListener(listener);
    }

    public void setOnViewCloseLogButton(ActionListener listener) {
        viewCloseLogButton.addActionListener(listener);
    }

}
