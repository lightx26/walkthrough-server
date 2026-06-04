package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit.listeners;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules._shared.infra.messaging.RiskScanEventMessage;
import com.pet.walkthroughserver.modules.riskzone.business.sync.RiskScanCommand;
import com.pet.walkthroughserver.modules.riskzone.business.sync.RiskScanHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskScanRabbitListener {

    private final RiskScanHandler handler;

    @RabbitListener(queues = RabbitMQConfig.RISK_SCAN_QUEUE)
    public void onMessage(RiskScanEventMessage message) {
        handler.handle(new RiskScanCommand(message.getScanId()));
    }
}
