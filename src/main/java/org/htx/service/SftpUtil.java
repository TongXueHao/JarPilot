package org.htx.service;

import com.jcraft.jsch.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
