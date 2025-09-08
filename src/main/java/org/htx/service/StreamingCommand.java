package org.htx.service;

import com.intellij.openapi.diagnostic.Logger;
import net.schmizz.sshj.connection.channel.direct.Session;
import java.util.concurrent.Future;

public class StreamingCommand {
    private static final Logger LOG = Logger.getInstance(StreamingCommand.class);

    private final Session session;
    private final Session.Command command;
    private final Future<?> future;

    public StreamingCommand(Session session, Session.Command command, Future<?> future) {
        this.session = session;
        this.command = command;
        this.future = future;
    }

    public void close() {
        try {
            command.getInputStream().close(); // 触发 readLine 返回
        } catch (Exception ignored) {}
        try {
            command.close();
        } catch (Exception ignored) {}
        try {
            session.close();
        } catch (Exception ignored) {}

        if (future != null && !future.isDone()) {
            future.cancel(true); // 中断 pooledThread
        }
    }
}
