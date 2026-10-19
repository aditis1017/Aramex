package com.customcharge.persistencelayer.export.web;

import com.customcharge.persistencelayer.export.event.ExportProgressEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mvp.persistence.export", name = "enabled", havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExportProgressSseBridge {

    private final ExportSseHub exportSseHub;

    public ExportProgressSseBridge(ExportSseHub exportSseHub) {
        this.exportSseHub = exportSseHub;
    }

    @EventListener
    public void onExportProgress(ExportProgressEvent event) {
        exportSseHub.broadcast(event.payload());
    }
}
