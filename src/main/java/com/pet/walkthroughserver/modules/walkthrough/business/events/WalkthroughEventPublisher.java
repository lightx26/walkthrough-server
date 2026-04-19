package com.pet.walkthroughserver.modules.walkthrough.business.events;

public interface WalkthroughEventPublisher {
    void publish(WalkthroughEvent event);
}
