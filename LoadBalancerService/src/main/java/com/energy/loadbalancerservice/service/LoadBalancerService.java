package com.energy.loadbalancerservice.service;

import com.energy.loadbalancerservice.config.RabbitMQConfig;
import com.energy.loadbalancerservice.dto.SensorDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoadBalancerService {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerService.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${loadbalancer.replica.count:3}")
    private int replicaCount;

    private long messagesProcessed = 0;
    private long[] replicaMessageCounts;

    @Autowired
    public LoadBalancerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.replicaMessageCounts = new long[]{0, 0, 0};
    }

    @RabbitListener(queues = RabbitMQConfig.CENTRAL_QUEUE)
    public void consumeDeviceData(SensorDataDTO sensorData) {
        messagesProcessed++;

        log.debug("Received message #{}: deviceId={}, timestamp={}, value={}",
                messagesProcessed,
                sensorData.getDeviceId(),
                sensorData.getTimestamp(),
                sensorData.getMeasurementValue());

        int replicaIndex = selectReplica(sensorData.getDeviceId());

        String ingestQueue = RabbitMQConfig.INGEST_QUEUE_PREFIX + (replicaIndex + 1);
        rabbitTemplate.convertAndSend(ingestQueue, sensorData);

        replicaMessageCounts[replicaIndex]++;

        log.info("Message #{} from device {} → {} (Replica {}). Distribution: [R1:{}, R2:{}, R3:{}]",
                messagesProcessed,
                sensorData.getDeviceId(),
                ingestQueue,
                replicaIndex + 1,
                replicaMessageCounts[0],
                replicaMessageCounts[1],
                replicaMessageCounts[2]);
    }

    private int selectReplica(UUID deviceId) {
        int hash = deviceId.hashCode();

        int positiveHash = hash & Integer.MAX_VALUE;

        int replicaIndex = positiveHash % replicaCount;
        log.debug("Consistent hashing: deviceId={} → hash={} → replica={}",
                deviceId, hash, replicaIndex);

        return replicaIndex;
    }
}
