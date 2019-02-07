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
package com.nordstrom.gatekeeper;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ParametersAreNonnullByDefault
public class GatekeeperDao {

  private AmazonDynamoDB client;
  private final AtomicBoolean tableGuarantee = new AtomicBoolean(false);
  @Nullable private final String localDynamoHost;
  private final GatekeeperDbCacheManager cacheManager;

  public GatekeeperDao(
      @Nullable String localDynamoHost, GatekeeperDbCacheManager gatekeeperDbCacheManager) {
    this.localDynamoHost = localDynamoHost;
    this.cacheManager = gatekeeperDbCacheManager;
  }

  public void createSubjectPermissions(String subjectId, String... permissions) {
    createSubjectPermissions(subjectId, Lists.newArrayList(permissions), Collections.emptyList());
  }

  public void createSubjectRoles(String subjectId, String... roles) {
    createSubjectPermissions(subjectId, Collections.emptyList(), Lists.newArrayList(roles));
  }

  public void createSubjectPermissions(
      String subjectId, List<String> permissions, List<String> roles) {
    guaranteeTables();
    cacheManager.bustAuthorizationCache();
    PutItemRequest request =
        new PutItemRequest()
            .withTableName("subjects")
            .addItemEntry("subject-id", new AttributeValue(subjectId))
            .withExpressionAttributeNames(Collections.singletonMap("#subject", "subject-id"))
            .withConditionExpression("attribute_not_exists(#subject)");

    if (!permissions.isEmpty()) {
      request.addItemEntry("permissions", new AttributeValue().withSS(permissions));
    }
    if (!roles.isEmpty()) {
      request.addItemEntry("roles", new AttributeValue().withSS(roles));
    }
    getClient().putItem(request);
  }

  public void addSubjectPermissions(String subjectId, String... permissions) {
    if (permissions.length > 0) {
      guaranteeTables();
      cacheManager.bustAuthorizationCache();
      UpdateItemRequest request =
          new UpdateItemRequest()
              .withTableName("subjects")
              .addKeyEntry("subject-id", new AttributeValue(subjectId))
              .withExpressionAttributeValues(
                  Collections.singletonMap(":item", new AttributeValue().withSS(permissions)))
              .withExpressionAttributeNames(
                  new ImmutableMap.Builder<String, String>()
                      .put("#subject", "subject-id")
                      .put("#permissionset", "permissions")
                      .build())
              .withConditionExpression("attribute_exists(#subject)")
              .withUpdateExpression("ADD #permissionset :item");
      getClient().updateItem(request);
    }
  }

  public void addSubjectRoles(String subjectId, String... roles) {
    if (roles.length > 0) {
      guaranteeTables();
      cacheManager.bustAuthorizationCache();
      UpdateItemRequest request =
          new UpdateItemRequest()
              .withTableName("subjects")
              .addKeyEntry("subject-id", new AttributeValue(subjectId))
              .withExpressionAttributeValues(
                  Collections.singletonMap(":item", new AttributeValue().withSS(roles)))
              .withExpressionAttributeNames(
                  new ImmutableMap.Builder<String, String>()
                      .put("#subject", "subject-id")
                      .put("#roleset", "roles")
                      .build())
              .withConditionExpression("attribute_exists(#subject)")
              .withUpdateExpression("ADD #roleset :item");
      getClient().updateItem(request);
    }
  }

  public void removeSubjectPermissions(String subjectId, String... permissions) {
    if (permissions.length > 0) {
      guaranteeTables();
      cacheManager.bustAuthorizationCache();
      UpdateItemRequest request =
          new UpdateItemRequest()
              .withTableName("subjects")
              .addKeyEntry("subject-id", new AttributeValue(subjectId))
              .withExpressionAttributeValues(
                  Collections.singletonMap(":item", new AttributeValue().withSS(permissions)))
              .withExpressionAttributeNames(
                  new ImmutableMap.Builder<String, String>()
                      .put("#subject", "subject-id")
                      .put("#permissionset", "permissions")
                      .build())
              .withConditionExpression("attribute_exists(#subject)")
              .withUpdateExpression("DELETE #permissionset :item");
      getClient().updateItem(request);
    }
  }

  public void removeSubjectRoles(String subjectId, String... roles) {
    if (roles.length > 0) {
      guaranteeTables();
      cacheManager.bustAuthorizationCache();
      UpdateItemRequest request =
          new UpdateItemRequest()
              .withTableName("subjects")
              .addKeyEntry("subject-id", new AttributeValue(subjectId))
              .withExpressionAttributeValues(
                  Collections.singletonMap(":item", new AttributeValue().withSS(roles)))
              .withExpressionAttributeNames(
                  new ImmutableMap.Builder<String, String>()
                      .put("#subject", "subject-id")
                      .put("#roleset", "roles")
                      .build())
              .withConditionExpression("attribute_exists(#subject)")
              .withUpdateExpression("DELETE #roleset :item");
      getClient().updateItem(request);
    }
  }

  public void createRolesPermissions(String roleId, String... permissions) {
    if (permissions.length > 0) {
      guaranteeTables();
      PutItemRequest request =
          new PutItemRequest()
              .withTableName("role-permissions")
              .addItemEntry("role-id", new AttributeValue(roleId))
              .addItemEntry("permissions", new AttributeValue().withSS(permissions))
              .withExpressionAttributeNames(Collections.singletonMap("#role", "role-id"))
              .withConditionExpression("attribute_not_exists(#role)");
      getClient().putItem(request);
    }
  }

  public void addRolePermissions(String roleId, String... permissions) {
    if (permissions.length > 0) {
      guaranteeTables();
      cacheManager.bustAuthorizationCache();
      UpdateItemRequest request =
          new UpdateItemRequest()
              .withTableName("role-permissions")
              .addKeyEntry("role-id", new AttributeValue(roleId))
              .withExpressionAttributeValues(
                  Collections.singletonMap(":item", new AttributeValue().withSS(permissions)))
              .withExpressionAttributeNames(
                  new ImmutableMap.Builder<String, String>()
                      .put("#role", "role-id")
                      .put("#permissionset", "permissions")
                      .build())
              .withConditionExpression("attribute_exists(#role)")
              .withUpdateExpression("ADD #permissionset :item");
      getClient().updateItem(request);
    }
  }

  public void removeRolePermissions(String roleId, String... permissions) {
    guaranteeTables();
    if (permissions.length > 0) {
      guaranteeTables();
      cacheManager.bustAuthorizationCache();
      UpdateItemRequest request =
          new UpdateItemRequest()
              .withTableName("role-permissions")
              .addKeyEntry("role-id", new AttributeValue(roleId))
              .withExpressionAttributeValues(
                  Collections.singletonMap(":item", new AttributeValue().withSS(permissions)))
              .withExpressionAttributeNames(
                  new ImmutableMap.Builder<String, String>()
                      .put("#role", "role-id")
                      .put("#permissionset", "permissions")
                      .build())
              .withConditionExpression("attribute_exists(#role)")
              .withUpdateExpression("DELETE #permissionset :item");
      getClient().updateItem(request);
    }
  }

  private Set<String> extractSS(GetItemResult result, String key) {
    return Optional.ofNullable(result.getItem())
        .flatMap(item -> Optional.ofNullable(item.get(key)))
        .flatMap(perms -> Optional.ofNullable(perms.getSS()))
        .map(Sets::newHashSet)
        .orElse(Sets.newHashSet());
  }

  public Set<String> getRolePermissions(String roleId) {
    guaranteeTables();
    GetItemResult result =
        getClient()
            .getItem(
                new GetItemRequest()
                    .withTableName("role-permissions")
                    .withKey(Collections.singletonMap("role-id", new AttributeValue(roleId))));
    return extractSS(result, "permissions");
  }

  public Authorization getSubjectAuthorization(String subjectId) {
    guaranteeTables();
    GetItemResult result =
        getClient()
            .getItem(
                new GetItemRequest()
                    .withTableName("subjects")
                    .withKey(
                        Collections.singletonMap("subject-id", new AttributeValue(subjectId))));
    Set<String> permissions = extractSS(result, "permissions");
    Set<String> roles = extractSS(result, "roles");
    return new Authorization(subjectId, permissions, roles);
  }

  private String getAwsRegion() {
    return Optional.ofNullable(System.getenv("AWS_DEFAULT_REGION"))
        .filter(envVar -> !envVar.isEmpty())
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
  public void guaranteeTables() {
    if (!tableGuarantee.getAndSet(true)) {
      Set<String> names = new HashSet<>(getClient().listTables().getTableNames());
      if (!names.contains("role-permissions")) {
        createRolePermissionsTable();
      }
      if (!names.contains("subjects")) {
        createSubjectPermissionsTable();
      }
    }
  }

  private void createRolePermissionsTable() {
    getClient()
        .createTable(
            new CreateTableRequest()
                .withTableName("role-permissions")
                .withAttributeDefinitions(new AttributeDefinition("role-id", ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement("role-id", KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L)));
  }

  private void createSubjectPermissionsTable() {
    getClient()
        .createTable(
            new CreateTableRequest()
                .withTableName("subjects")
                .withAttributeDefinitions(
                    new AttributeDefinition("subject-id", ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement("subject-id", KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L)));
  }

  private AmazonDynamoDB getClient() {
    if (client == null) {
      client = createClient();
    }
    return client;
  }

  public void deleteTables() {
    try {
      cacheManager.bustAuthorizationCache();
      getClient().deleteTable("role-permissions");
      getClient().deleteTable("subjects");
    } catch (ResourceNotFoundException e) {
      log.error("tables do not exist", e);
    }
    tableGuarantee.set(false);
  }
}
