package org.htx.service;

import com.intellij.openapi.application.ApplicationManager;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ConnectionUtils {

    private SSHClient sshClient;

    public void connect(String ip, int port, String username, String password) throws IOException {
        if (sshClient != null && sshClient.isConnected()) {
            return;
        }

        sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(ip, port);
        sshClient.authPassword(username, password);
        sshClient.useCompression();
        sshClient.setConnectTimeout(3000);
    }

    /**
     * exec a command on a remote host over SSH and handle output with a callback.
     *
     * @param cmd      command to execute
     * @param callback callback function to handle each line of output
     * @throws Exception
     */
    public void exec(String cmd,Consumer<String> callback) throws Exception {
        checkConnection();
        try (Session session = sshClient.startSession()) {
            session.allocateDefaultPTY();
            try (Session.Command exec = session.exec(cmd)) {

                if (callback != null) {
                    InputStream inputStream = exec.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        callback.accept(line);
                    }
                }

                exec.join();
            }
        }
    }

    public String exec(String cmd) throws Exception {
        checkConnection();
        try (Session session = sshClient.startSession()) {

            try (Session.Command exec = session.exec(cmd)) {
                ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
                ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

                Thread outThread = new Thread(() -> {
                    try (InputStream stdout = exec.getInputStream()) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = stdout.read(buf)) != -1) {
                            outBuffer.write(buf, 0, len);
                        }
                    } catch (IOException ignored) {}
                });

                Thread errThread = new Thread(() -> {
                    try (InputStream stderr = exec.getErrorStream()) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = stderr.read(buf)) != -1) {
                            errBuffer.write(buf, 0, len);
                        }
                    } catch (IOException ignored) {}
                });

                outThread.start();
                errThread.start();

                // 等待命令结束（带超时，避免卡死）
                exec.join(30, TimeUnit.SECONDS);

                outThread.join();
                errThread.join();

                int exitStatus = exec.getExitStatus();
                String output = outBuffer.toString(StandardCharsets.UTF_8);
                String errorOutput = errBuffer.toString(StandardCharsets.UTF_8);

                if (exitStatus != 0) {
                    throw new RuntimeException(
                            "命令执行失败: " + cmd +
                                    "\nExit code: " + exitStatus +
                                    "\nError: " + errorOutput);
                }

                return output.isEmpty() ? errorOutput : output;
            }
        }
    }

    public void execStream(String cmd, Consumer<String> outputHandler) throws Exception {
        checkConnection();
        Session session = sshClient.startSession();
        Session.Command exec = session.exec(cmd);

        // 提交到插件线程池
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exec.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputHandler.accept(line);
                }
            } catch (Exception ignored) {}
        });

    }

    private void checkConnection() {
        if (sshClient == null || !sshClient.isConnected()) {
            throw new IllegalStateException("SSHClient is not connected. Call connect() first.");
        }
    }

    public void close() {
        if (sshClient != null) {
            try {
                sshClient.disconnect();
                sshClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
