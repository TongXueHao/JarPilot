package org.htx.service;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

public class ConnectionNotifier {

    private static final String GROUP_ID = "SSH Connections";

    /**
     * Show a notification for SSH connection result.
     *
     * @param project the current project
     * @param success true if connection succeeded, false otherwise
     */
    public static void notifyConnectionResult(Project project, boolean success, String msg) {
        String title = "SSH connection";
        NotificationType type = success ? NotificationType.INFORMATION : NotificationType.ERROR;
        Notification notification = new Notification(GROUP_ID, title, msg, type);
        Notifications.Bus.notify(notification, project);
    }

}
