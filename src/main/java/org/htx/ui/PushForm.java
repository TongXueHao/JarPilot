package org.htx.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class PushForm {

    private final JBTextField localJarField = new JBTextField(20);
    private final JBTextField remoteJarField = new JBTextField(20);

    private final JButton uploadButton = new JButton("Upload");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel uploadInfo = new JLabel("0% / 0 KB/s");
    private final JPanel mainPanel;

    public PushForm(Project project) {

        localJarField.getEmptyText().setText("Local JAR Path");
        remoteJarField.getEmptyText().setText("Remote JAR Path");

        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        mainPanel.add(new JLabel("Local JAR:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(localJarField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        mainPanel.add(new JLabel("Remote Path:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(remoteJarField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.PAGE_START;
        mainPanel.add(uploadButton, gbc);
        uploadButton.setEnabled(false);

        JPanel progressPanel = new JPanel(new BorderLayout(5, 0));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(uploadInfo, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;

        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setForeground(new JBColor(new Color(0, 122, 204), new Color(0, 122, 204)));
        progressBar.setPreferredSize(new Dimension(200, 4));
        mainPanel.add(progressPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        setupValidation(project);
    }


    private void setupValidation(Project project) {
        new ComponentValidator(project).withValidator(() -> {
            String path = localJarField.getText();
            if (StringUtil.isEmpty(path)) {
                updateUploadButton(false);
                return new ValidationInfo("Local JAR path cannot be empty", localJarField);
            }
            File file = new File(path);
            if (!file.exists() || !file.isFile() || !path.endsWith(".jar")) {
                updateUploadButton(false);
                return new ValidationInfo("Local path must be an existing .jar file", localJarField);
            }
            updateUploadButton(checkAllValid());
            return null;
        }).installOn(localJarField);

        localJarField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                ComponentValidator.getInstance(localJarField)
                        .ifPresent(ComponentValidator::revalidate);
            }
        });

        new ComponentValidator(project).withValidator(() -> {
            String remote = remoteJarField.getText();
            if (StringUtil.isEmpty(remote)) {
                updateUploadButton(false);
                return new ValidationInfo("Remote JAR path cannot be empty", remoteJarField);
            }
            try {
                Paths.get(remote); // 如果能解析成路径，则合法
            } catch (InvalidPathException e) {
                updateUploadButton(false);
                return new ValidationInfo("Remote JAR path is not a valid path", remoteJarField);
            }

            updateUploadButton(checkAllValid());
            return null;
        }).installOn(remoteJarField);

        remoteJarField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                ComponentValidator.getInstance(remoteJarField)
                        .ifPresent(ComponentValidator::revalidate);
            }
        });

    }

    private boolean checkAllValid() {
        String local = localJarField.getText();
        String remote = remoteJarField.getText();
        if (StringUtil.isEmpty(local) || StringUtil.isEmpty(remote)) {
            return false;
        }
        File file = new File(local);
        return file.exists() && file.isFile() && local.endsWith(".jar");
    }

    public void updateUploadButton(boolean enabled) {
        uploadButton.setEnabled(enabled);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void setOnUpload(ActionListener listener) {
        uploadButton.addActionListener(listener);
    }

    public String getLocalJarPath() {
        return localJarField.getText();
    }

    public String getRemotePath() {
        return remoteJarField.getText();
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getUploadInfo() {
        return uploadInfo;
    }
}
