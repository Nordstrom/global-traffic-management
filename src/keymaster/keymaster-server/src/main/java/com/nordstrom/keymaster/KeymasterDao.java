/**
 * Copyright (C) 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nordstrom.keymaster;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.nordstrom.keymaster.CertificateRequest.DataClassification;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ParametersAreNonnullByDefault
public class KeymasterDao {
  // TODO(JL): Tables should be created with CloudFormation. GuaranteeTables should be just for
  // testing.
  // If tables here, don't do anything. Create/Update: If tables aren't there, call should just
  // fail.

  private AmazonDynamoDB client;
  private final AtomicBoolean tableGuarantee = new AtomicBoolean(false);
  @Nullable private final String localDynamoHost;

  public KeymasterDao(@Nullable String localDynamoHost) {
    this.localDynamoHost = localDynamoHost;
  }

  public BigInteger handleNewKeypair(String subjectId, DataClassification dataClassification)
      throws InterruptedException {
    int decimalRadix = 10;
    String serialCounter = getKeypairSerialCounter(subjectId, dataClassification);
    if (serialCounter.equals("")) {
      serialCounter = createKeypairSerialCounter(subjectId, dataClassification);
    } else {
      serialCounter = updateKeypairSerialCounter(subjectId, dataClassification);
    }

    return new BigInteger(serialCounter, decimalRadix);
  }

  private String createKeypairSerialCounter(String subjectId, DataClassification dataClassification)
      throws InterruptedException {
    guaranteeTables();
    String newSerialCounter = "1";

    PutItemRequest request =
        new PutItemRequest()
            .withTableName("keypair-serial-counters")
            .addItemEntry("subject-id", new AttributeValue(subjectId))
            .addItemEntry("data-classification", new AttributeValue(dataClassification.name()))
            .addItemEntry("serial-counter", new AttributeValue().withN(newSerialCounter))
            .withExpressionAttributeNames(Collections.singletonMap("#subject", "subject-id"))
            .withConditionExpression("attribute_not_exists(#subject)");

    getClient().putItem(request);
    return newSerialCounter;
  }

  private String getKeypairSerialCounter(String subjectId, DataClassification dataClassification) {
    GetItemRequest request =
        new GetItemRequest()
            .withTableName("keypair-serial-counters")
            .addKeyEntry("subject-id", new AttributeValue(subjectId))
            .addKeyEntry("data-classification", new AttributeValue(dataClassification.name()));

    GetItemResult result = getClient().getItem(request);
    return extractN(result, "serial-counter");
  }

  private String updateKeypairSerialCounter(String subjectId, DataClassification dataClassification)
      throws InterruptedException {
    guaranteeTables();

    UpdateItemRequest request =
        new UpdateItemRequest()
            .withTableName("keypair-serial-counters")
            .addKeyEntry("subject-id", new AttributeValue(subjectId))
            .addKeyEntry("data-classification", new AttributeValue(dataClassification.name()))
            .withExpressionAttributeValues(
                Collections.singletonMap(":incr", new AttributeValue().withN("1")))
            .withUpdateExpression("SET #sc = #sc + :incr")
            .withExpressionAttributeNames(
                new ImmutableMap.Builder<String, String>()
                    .put("#sid", "subject-id")
                    .put("#dtc", "data-classification")
                    .put("#sc", "serial-counter")
                    .build())
            .withConditionExpression("attribute_exists(#sid) AND attribute_exists(#dtc)")
            .withReturnValues("ALL_NEW");

    UpdateItemResult result = getClient().updateItem(request);
    return extractN(result, "serial-counter");
  }

  private String extractN(GetItemResult result, String key) {
    return Optional.ofNullable(result.getItem())
        .flatMap(item -> Optional.ofNullable(item.get(key)))
        .flatMap(value -> Optional.ofNullable(value.getN()))
        .orElse("");
  }

  private String extractN(UpdateItemResult result, String key) {
    return Optional.ofNullable(result.getAttributes())
        .flatMap(item -> Optional.ofNullable(item.get(key)))
        .flatMap(value -> Optional.ofNullable(value.getN()))
        .orElse("");
  }

  private String extractS(GetItemResult result, String key) {
    return Optional.ofNullable(result.getItem())
        .flatMap(item -> Optional.ofNullable(item.get(key)))
        .flatMap(value -> Optional.ofNullable(value.getS()))
        .orElse("");
  }

  private String getAwsRegion() {
    return Optional.ofNullable(System.getenv("AWS_DEFAULT_REGION"))
        .filter(it -> !it.isEmpty())
        .orElse("us-west-2");
  }

  private AmazonDynamoDB localDynamo(String host) {
    log.warn("using local dynamo host:port - {}:8000", host);
    String hostPort = String.format("http://%s:8000", host);
    return AmazonDynamoDBClientBuilder.standard()
        .withCredentials(
            new AWSCredentialsProvider() {
              @Override
              public AWSCredentials getCredentials() {
                return new AWSCredentials() {
                  @Override
                  public String getAWSAccessKeyId() {
                    return "fake";
                  }

                  @Override
                  public String getAWSSecretKey() {
                    return "fake";
                  }
                };
              }

              @Override
              public void refresh() {}
            })
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(hostPort, ""))
        .build();
  }

  private AmazonDynamoDB createClient() {
    return Optional.ofNullable(localDynamoHost)
        .map(this::localDynamo)
        .orElseGet(
            () -> {
              String region = getAwsRegion();
              log.warn("using AWS dynamo with region: {}", region);
              return AmazonDynamoDBClientBuilder.standard()
                  .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                  .withRegion(region)
                  .build();
            });
  }

  @VisibleForTesting
  public void guaranteeTables() throws InterruptedException {
    if (!tableGuarantee.getAndSet(true)) {
      Set<String> names = new HashSet<>(getClient().listTables().getTableNames());
      if (!names.contains("keypair-serial-counters")) {
        createKeypairSerialCountersTable();
      }
    }
  }

  private void createKeypairSerialCountersTable() throws InterruptedException {
    CreateTableRequest request =
        new CreateTableRequest()
            .withTableName("keypair-serial-counters")
            .withAttributeDefinitions(
                new AttributeDefinition("subject-id", ScalarAttributeType.S),
                new AttributeDefinition("data-classification", ScalarAttributeType.S))
            .withKeySchema(
                new KeySchemaElement("subject-id", KeyType.HASH),
                new KeySchemaElement("data-classification", KeyType.RANGE))
            .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L));

    getClient().createTable(request);
    TableUtils.waitUntilActive(getClient(), "keypair-serial-counters");
  }

  private AmazonDynamoDB getClient() {
    if (client == null) {
      client = createClient();
    }
    return client;
  }

  public void deleteTables() {
    try {
      getClient().deleteTable("keypair-serial-counters");
    } catch (ResourceNotFoundException e) {
      log.error("tables do not exist", e);
    }
    tableGuarantee.set(false);
  }
}
