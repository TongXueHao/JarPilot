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

import com.jcraft.jsch.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Utility class for SFTP file transfers using JSch.
 *
 * @Author Hao Tong Xue
 * @Date 2025/8/27 15:30
 * @Version 1.0
 */
public class SftpUtil {

    public static void upload(String host, int port, String user, String password,
                              String local, String remote, SftpProgressMonitor monitor) throws Exception {

        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftp = (ChannelSftp) channel;

        File file = new File(local);
        try (FileInputStream fis = new FileInputStream(file)) {
            sftp.put(fis, remote, monitor);
        }

        sftp.disconnect();
        session.disconnect();
    }

    public static void download(String host, int port, String user, String password,
                                String remote, String local, SftpProgressMonitor monitor) throws Exception {

        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftp = (ChannelSftp) channel;

        try (FileOutputStream fos = new FileOutputStream(local)) {
            sftp.get(remote, fos, monitor);
        }

        sftp.disconnect();
        session.disconnect();
    }
}
