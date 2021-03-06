/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.resources.crd;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaTopicList;
import io.strimzi.api.kafka.model.KafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopicBuilder;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.test.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.strimzi.systemtest.resources.ResourceManager;

import java.util.function.Consumer;

import static io.strimzi.systemtest.enums.CustomResourceStatus.Ready;

public class KafkaTopicResource {
    private static final Logger LOGGER = LogManager.getLogger(KafkaTopicResource.class);

    public static final String PATH_TO_KAFKA_TOPIC_CONFIG = TestUtils.USER_PATH + "/../examples/topic/kafka-topic.yaml";

    public static MixedOperation<KafkaTopic, KafkaTopicList, Resource<KafkaTopic>> kafkaTopicClient() {
        return Crds.topicV1Beta2Operation(ResourceManager.kubeClient().getClient());
    }

    public static KafkaTopicBuilder topic(String clusterName, String topicName) {
        return defaultTopic(clusterName, topicName, 1, 1, 1);
    }

    public static KafkaTopicBuilder topic(String clusterName, String topicName, int partitions) {
        return defaultTopic(clusterName, topicName, partitions, 1, 1);
    }

    public static KafkaTopicBuilder topic(String clusterName, String topicName, int partitions, int replicas) {
        return defaultTopic(clusterName, topicName, partitions, replicas, replicas);
    }

    public static KafkaTopicBuilder topic(String clusterName, String topicName, int partitions, int replicas, int minIsr) {
        return defaultTopic(clusterName, topicName, partitions, replicas, minIsr);
    }

    public static KafkaTopicBuilder defaultTopic(String clusterName, String topicName, int partitions, int replicas, int minIsr) {
        KafkaTopic kafkaTopic = getKafkaTopicFromYaml(PATH_TO_KAFKA_TOPIC_CONFIG);
        return new KafkaTopicBuilder(kafkaTopic)
            .withNewMetadata()
                .withName(topicName)
                .withNamespace(ResourceManager.kubeClient().getNamespace())
                .addToLabels(Labels.STRIMZI_CLUSTER_LABEL, clusterName)
            .endMetadata()
            .editSpec()
                .withPartitions(partitions)
                .withReplicas(replicas)
                .addToConfig("min.insync.replicas", minIsr)
            .endSpec();
    }

    public static KafkaTopic createAndWaitForReadiness(KafkaTopic topic) {
        kafkaTopicClient().inNamespace(topic.getMetadata().getNamespace()).createOrReplace(topic);
        LOGGER.info("Created KafkaTopic {}", topic.getMetadata().getName());
        return waitFor(deleteLater(topic));
    }

    public static KafkaTopic topicWithoutWait(KafkaTopic kafkaTopic) {
        kafkaTopicClient().inNamespace(ResourceManager.kubeClient().getNamespace()).createOrReplace(kafkaTopic);
        return kafkaTopic;
    }

    private static KafkaTopic getKafkaTopicFromYaml(String yamlPath) {
        return TestUtils.configFromYaml(yamlPath, KafkaTopic.class);
    }

    private static KafkaTopic waitFor(KafkaTopic kafkaTopic) {
        return ResourceManager.waitForResourceStatus(kafkaTopicClient(), kafkaTopic, Ready);
    }

    private static KafkaTopic deleteLater(KafkaTopic kafkaTopic) {
        return ResourceManager.deleteLater(kafkaTopicClient(), kafkaTopic);
    }

    public static void replaceTopicResource(String resourceName, Consumer<KafkaTopic> editor) {
        ResourceManager.replaceCrdResource(KafkaTopic.class, KafkaTopicList.class, resourceName, editor);
    }
}
