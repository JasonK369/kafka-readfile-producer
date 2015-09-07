/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.sakserv.kafka.producer;

import com.github.sakserv.kafka.consumer.KafkaTestConsumer;
import com.github.sakserv.kafka.producer.config.ConfigVars;
import com.github.sakserv.propertyparser.PropertyParser;
import com.github.sakserv.minicluster.impl.KafkaLocalBroker;
import com.github.sakserv.minicluster.impl.ZookeeperLocalCluster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class KafkaReadfileProducerIntegrationTest {

    // Logger
    private static final Logger LOG = LoggerFactory.getLogger(KafkaReadfileProducer.class);

    // Setup the property parser
    private static PropertyParser propertyParser;
    static {
        try {
            propertyParser = new PropertyParser(ConfigVars.DEFAULT_PROPS_FILE);
            propertyParser.parsePropsFile();
        } catch(IOException e) {
            LOG.error("Unable to load property file: " + propertyParser.getProperty(ConfigVars.DEFAULT_PROPS_FILE));
        }
    }

    private static ZookeeperLocalCluster zookeeperLocalCluster;
    private static KafkaLocalBroker kafkaLocalBroker;

    private static String inputFile = "test/main/resources/test_input.txt";

    @BeforeClass
    public static void setUp() throws Exception {
        zookeeperLocalCluster = new ZookeeperLocalCluster.Builder()
                .setPort(Integer.parseInt(propertyParser.getProperty(ConfigVars.ZOOKEEPER_PORT_KEY)))
                .setTempDir(propertyParser.getProperty(ConfigVars.ZOOKEEPER_TEMP_DIR_KEY))
                .setZookeeperConnectionString(propertyParser.getProperty(ConfigVars.ZOOKEEPER_CONNECTION_STRING_KEY))
                .build();
        zookeeperLocalCluster.start();

        kafkaLocalBroker = new KafkaLocalBroker.Builder()
                .setKafkaHostname(propertyParser.getProperty(ConfigVars.KAFKA_HOSTNAME_KEY))
                .setKafkaPort(Integer.parseInt(propertyParser.getProperty(ConfigVars.KAFKA_PORT_KEY)))
                .setKafkaBrokerId(Integer.parseInt(propertyParser.getProperty(ConfigVars.KAFKA_TEST_BROKER_ID_KEY)))
                .setKafkaProperties(new Properties())
                .setKafkaTempDir(propertyParser.getProperty(ConfigVars.KAFKA_TEST_TEMP_DIR_KEY))
                .setZookeeperConnectionString(propertyParser.getProperty(ConfigVars.ZOOKEEPER_CONNECTION_STRING_KEY))
                .build();
        kafkaLocalBroker.start();

    }

    @AfterClass
    public static void tearDown() throws Exception {

        kafkaLocalBroker.stop();
        zookeeperLocalCluster.stop();
    }

    @Test
    public void testKafkaReadfileProducer() throws Exception {

        // Producer
        KafkaReadfileProducer kafkaReadfileProducer = new KafkaReadfileProducer.Builder()
                .setKafkaHostname(propertyParser.getProperty(ConfigVars.KAFKA_HOSTNAME_KEY))
                .setKafkaPort(Integer.parseInt(propertyParser.getProperty(ConfigVars.KAFKA_PORT_KEY)))
                .setTopic(propertyParser.getProperty(ConfigVars.KAFKA_TOPIC_KEY))
                .setInputFileName(inputFile)
                .build();

        kafkaReadfileProducer.produceMessages();

        // Consumer
        List<String> seeds = new ArrayList<String>();
        seeds.add(kafkaLocalBroker.getKafkaHostname());
        KafkaTestConsumer kafkaTestConsumer = new KafkaTestConsumer();
        kafkaTestConsumer.consumeMessages(
                Integer.parseInt(propertyParser.getProperty(ConfigVars.KAFKA_TEST_MESSAGE_COUNT_KEY)),
                propertyParser.getProperty(ConfigVars.KAFKA_TOPIC_KEY),
                0,
                seeds,
                kafkaLocalBroker.getKafkaPort());

        // Assert num of messages produced = num of message consumed
        assertEquals(Long.parseLong(propertyParser.getProperty(ConfigVars.KAFKA_TEST_MESSAGE_COUNT_KEY)),
                kafkaTestConsumer.getNumRead());

    }


}
