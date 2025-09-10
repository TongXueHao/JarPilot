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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.htx.service.PersistentStateService;
import org.htx.service.RegexUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Form for managing the service (start/stop, view logs, etc.).
 *
 * @Author Hao Tong Xue
 * @Date 2025/8/27 10:00
 * @Version 1.0
 */
public class ServiceForm {
    private final JPanel mainPanel;
    private final JBTextField jarField;
    private final JBTextField portField;
    private final JBTextField logField;
    private final JBTextField activeField;
    private final JButton startStopButton = new JButton();
    private final JButton viewCloseLogButton = new JButton();

    public ServiceForm(@NotNull Project project, PersistentStateService.WindowState windowState) {
        jarField = new JBTextField(20);
        portField = new JBTextField(20);
        logField = new JBTextField(20);
        activeField = new JBTextField(20);

        jarField.setText(windowState.jarPath);
        portField.setText(windowState.serverPort + "");
        logField.setText(windowState.logPath);
        activeField.setText(windowState.activeField);

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

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Profiles active:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(activeField, gbc);

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
                    if (StringUtil.isEmpty(jarField.getText()) || !RegexUtil.isValidLinuxPath(logField.getText()) && !jarField.getText().endsWith(".jar")) {
                        return new ValidationInfo("JarField must be valid path with linux and end with .jar", jarField);
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
                    String pt = activeField.getText();
                    if (StringUtil.isNotEmpty(pt) && !RegexUtil.isValidProfile(pt)) {
                        return new ValidationInfo("Profiles active must be valid text", portField);
                    }
                    return null;
                }).installOn(activeField);
        activeField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull DocumentEvent e) {
                ComponentValidator.getInstance(activeField)
                        .ifPresent(ComponentValidator::revalidate);
            }
        });

        new ComponentValidator(project)
                .withValidator(() -> {

                    if(StringUtil.isEmpty(logField.getText()) || !RegexUtil.isValidLinuxPath(logField.getText())){
                        viewCloseLogButton.setEnabled(false);
                        return new ValidationInfo("Log path must be valid path with linux", logField);
                    }

                    viewCloseLogButton.setEnabled(true);
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

        if (StringUtil.isEmpty(jarField.getText()) || !jarField.getText().endsWith(".jar")) valid = false;

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

    public JBTextField getActiveField() {
        return activeField;
    }

    public void setOnStartStopButton(ActionListener listener) {
        startStopButton.addActionListener(listener);
    }

    public void setOnViewCloseLogButton(ActionListener listener) {
        viewCloseLogButton.addActionListener(listener);
    }

}
