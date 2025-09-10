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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

/**
 * Service to show notifications for SSH connection results.
 *
 * @Author Hao Tong Xue
 * @Date 2025/8/20 10:45
 * @Version 1.0
 */
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
