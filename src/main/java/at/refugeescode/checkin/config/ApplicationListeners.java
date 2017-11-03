package at.refugeescode.checkin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.RequestHandledEvent;

@Component
@Slf4j
public class ApplicationListeners {

    @EventListener
    public void handle(ContextStartedEvent event) {
        log.trace("Application context started.");
    }

    @EventListener
    public void handle(ContextRefreshedEvent event) {
        log.trace("Application context refreshed.");
    }

    @EventListener
    public void handle(ContextStoppedEvent event) {
        log.trace("Application context stopped.");
    }

    @EventListener
    public void handle(ContextClosedEvent event) {
        log.trace("Application context closed.");
    }

    @EventListener
    public void handle(RequestHandledEvent event) {
        log.trace("HTTP request handled: {}", event.getDescription());
    }
}
