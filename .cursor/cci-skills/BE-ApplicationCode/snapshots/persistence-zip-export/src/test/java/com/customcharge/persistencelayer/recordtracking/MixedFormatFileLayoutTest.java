package com.customcharge.persistencelayer.recordtracking;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class MixedFormatFileLayoutTest {

    private MixedFormatFileLayout layout;

    @AfterEach
    void tearDown() {
        if (layout != null && layout.isStarted()) {
            layout.stop();
        }
    }

    @Test
    void doLayout_shouldEmitJsonForTrackingLogger() {
        layout = new MixedFormatFileLayout();
        layout.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        layout.start();

        LoggingEvent event = new LoggingEvent();
        event.setLoggerName(RecordTrackingLogger.class.getName());
        event.setLevel(Level.INFO);
        event.setMessage("record_tracking");
        event.setTimeStamp(System.currentTimeMillis());

        String formatted = layout.doLayout(event);

        assertThat(formatted).contains("record_tracking");
        assertThat(formatted).endsWith(System.lineSeparator());
    }

    @Test
    void doLayout_shouldEmitPlainTextForOtherLoggers() {
        layout = new MixedFormatFileLayout();
        layout.setPlainPattern("%m%n");
        layout.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        layout.start();

        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("com.example.OtherLogger");
        event.setMessage("plain message");
        event.setTimeStamp(System.currentTimeMillis());

        assertThat(layout.doLayout(event)).isEqualTo("plain message" + System.lineSeparator());
    }
}
