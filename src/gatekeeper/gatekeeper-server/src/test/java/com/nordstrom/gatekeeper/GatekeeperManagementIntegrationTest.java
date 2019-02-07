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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.nordstrom.gatekeeper.grpc.AuthZ;
import com.nordstrom.gatekeeper.grpc.AuthZPermissions;
import com.nordstrom.gatekeeper.grpc.ChangeResult;
import com.nordstrom.gatekeeper.util.GatekeeperTestClient;
import com.nordstrom.gatekeeper.util.GatekeeperTestServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests the management / control plane for making Gatekeeper changes defined in:
 * https://gitlab.yourdomain.com/gtm/gtm-grpc/blob/master/gatekeeper/src/main/proto/gatekeeper_manage.proto
 */
class GatekeeperManagementIntegrationTest {

  @RegisterExtension static GatekeeperTestServer testServer = new GatekeeperTestServer();
  @RegisterExtension GatekeeperTestClient testClient = new GatekeeperTestClient();

  @Test
  void testCreateSubjectPermissions() throws Exception {
    // when subject id is created with permissions
    ChangeResult result =
        testClient.getClient().createSubjectPermissions("danny", "send", "receive");
    assertTrue(result.getSuccess());

    // then the subject has those permissions
    AuthZ authZ = testClient.getClient().listSubjectAuthZ("danny");
    assertEquals("danny", authZ.getId());
    assertEquals(2, authZ.getPermissionsList().size());
    assertTrue(authZ.getPermissionsList().contains("send"));
    assertTrue(authZ.getPermissionsList().contains("receive"));
  }

  @Test
  void testAddSubjectPermissions() throws Exception {
    // when subject id is created with permissions
    testClient.getClient().createSubjectPermissions("danny", "send", "receive");

    // when some permissions are added
    ChangeResult result = testClient.getClient().addSubjectPermissions("danny", "foo");
    assertTrue(result.getSuccess());

    // then the subject has those permissions
    AuthZ authZ = testClient.getClient().listSubjectAuthZ("danny");
    assertEquals("danny", authZ.getId());
    assertEquals(3, authZ.getPermissionsList().size());
    assertTrue(authZ.getPermissionsList().contains("send"));
    assertTrue(authZ.getPermissionsList().contains("receive"));
    assertTrue(authZ.getPermissionsList().contains("foo"));
  }

  @Test
  void testRemoveSubjectPermissions() throws Exception {
    // when subject id is created with permissions
    testClient.getClient().createSubjectPermissions("danny", "send", "receive", "foo");

    // when some permissions are removed
    ChangeResult result = testClient.getClient().removeSubjectPermissions("danny", "foo");
    assertTrue(result.getSuccess());

    // then the subject has those permissions
    AuthZ authZ = testClient.getClient().listSubjectAuthZ("danny");
    assertEquals("danny", authZ.getId());
    assertEquals(2, authZ.getPermissionsList().size());
    assertTrue(authZ.getPermissionsList().contains("send"));
    assertTrue(authZ.getPermissionsList().contains("receive"));
  }

  @Test
  void testCreateRolePermissions() throws Exception {
    // when role id is created with permissions
    ChangeResult result = testClient.getClient().createRolePermissions("email", "send", "receive");
    assertTrue(result.getSuccess());

    // then the role has those permissions
    AuthZPermissions permissions = testClient.getClient().listRolePermissions("email");
    assertEquals("email", permissions.getId());
    assertEquals(2, permissions.getPermissionsList().size());
    assertTrue(permissions.getPermissionsList().contains("send"));
    assertTrue(permissions.getPermissionsList().contains("receive"));
  }

  @Test
  void testAddRolePermissions() throws Exception {
    // given role id is created with permissions
    testClient.getClient().createRolePermissions("email", "send", "receive");

    // when a role permission is added
    ChangeResult result = testClient.getClient().addRolePermissions("email", "delete_spam");
    assertTrue(result.getSuccess());

    // then the role has those permissions
    AuthZPermissions permissions = testClient.getClient().listRolePermissions("email");
    assertEquals("email", permissions.getId());
    assertEquals(3, permissions.getPermissionsList().size());
    assertTrue(permissions.getPermissionsList().contains("send"));
    assertTrue(permissions.getPermissionsList().contains("receive"));
    assertTrue(permissions.getPermissionsList().contains("delete_spam"));
  }

  @Test
  void testRemoveRolePermissions() throws Exception {
    // given role id is created with permissions
    testClient.getClient().createRolePermissions("email", "send", "receive", "delete_spam");

    // when a role permission is removed
    ChangeResult result = testClient.getClient().removeRolePermissions("email", "delete_spam");
    assertTrue(result.getSuccess());

    // then the role has those permissions
    AuthZPermissions permissions = testClient.getClient().listRolePermissions("email");
    assertEquals("email", permissions.getId());
    assertEquals(2, permissions.getPermissionsList().size());
    assertTrue(permissions.getPermissionsList().contains("send"));
    assertTrue(permissions.getPermissionsList().contains("receive"));
  }

  @Test
  void testCreateSubjectPermissionsAndRoles() throws Exception {
    // when a subject id is created with permissions and roles
    ChangeResult result =
        testClient
            .getClient()
            .createSubjectPermissions(
                "danny",
                Lists.newArrayList("walk", "run"),
                Lists.newArrayList("emailer", "snailmailer"));
    assertTrue(result.getSuccess());

    AuthZ authZ = testClient.getClient().listSubjectAuthZ("danny");

    // then the subject has those permissions
    assertEquals("danny", authZ.getId());
    assertEquals(2, authZ.getPermissionsList().size());
    assertTrue(authZ.getPermissionsList().contains("walk"));
    assertTrue(authZ.getPermissionsList().contains("run"));

    // and the subject has those roles
    assertEquals("danny", authZ.getId());
    assertEquals(2, authZ.getRolesList().size());
    assertTrue(authZ.getRolesList().contains("emailer"));
    assertTrue(authZ.getRolesList().contains("snailmailer"));
  }
}
