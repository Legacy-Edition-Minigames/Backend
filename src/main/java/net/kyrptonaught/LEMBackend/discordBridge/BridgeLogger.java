package net.kyrptonaught.LEMBackend.discordBridge;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

public class BridgeLogger extends AbstractAppender {
    private static final PatternLayout layout = PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss} %level] (%logger{1}) %msg{nolookups}%n").build();

    protected BridgeLogger(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        if (event.getLevel() == Level.DEBUG || event.getLevel() == Level.TRACE) return;
        if (event.getLoggerName().equals("net.dv8tion.jda.api.requests.RestRateLimiter")) return;

        BridgeIn.handleServerLog(BridgeModule.logWebhook, layout.toSerializable(event));
    }
}
