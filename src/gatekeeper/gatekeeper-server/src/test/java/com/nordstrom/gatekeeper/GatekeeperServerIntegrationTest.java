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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import com.nordstrom.gatekeeper.util.GatekeeperTestClient;
import com.nordstrom.gatekeeper.util.GatekeeperTestServer;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests Gatekeeper Server implementation defined in:
 * https://gitlab.yourdomain.com/gtm/gtm-grpc/blob/master/gatekeeper/src/main/proto/gatekeeper.proto
 */
class GatekeeperServerIntegrationTest {

  @RegisterExtension static GatekeeperTestServer testServer = new GatekeeperTestServer();

  @RegisterExtension GatekeeperTestClient testClient = new GatekeeperTestClient();

  private TestScheduler testScheduler;

  @BeforeEach
  public void beforeEach() throws Exception {
    testScheduler = new TestScheduler();
    RxJavaPlugins.setIoSchedulerHandler((s) -> testScheduler);
  }

  @AfterEach
  public void afterEach() throws Exception {
    RxJavaPlugins.reset();
  }

  @Test
  public void testServer() throws Exception {
    // no permission set
    AuthorizationResponse response =
        testClient.getClient().authorize("nobody", "nothing", "something");

    assertTrue(response.hasSuccess());
    assertFalse(response.hasError());
    assertEquals(
        Sets.newHashSet("nothing", "something"),
        response.getSuccess().getPermissionsMap().keySet());
    assertEquals(Boolean.FALSE, response.getSuccess().getPermissionsMap().get("nothing"));
    assertEquals(Boolean.FALSE, response.getSuccess().getPermissionsMap().get("something"));
  }

  @Test
  public void testServerPermissions() throws Exception {
    testServer
        .getDao()
        .createSubjectPermissions("loopy louise", "jot on scratch pad", "meander about asylum");

    AuthorizationResponse response =
        testClient
            .getClient()
            .authorize("loopy louise", "scribble on wall", "meander about asylum");

    assertTrue(response.hasSuccess());
    assertFalse(response.hasError());
    assertEquals(
        Sets.newHashSet("scribble on wall", "meander about asylum"),
        response.getSuccess().getPermissionsMap().keySet());
    assertEquals(Boolean.FALSE, response.getSuccess().getPermissionsMap().get("scribble on wall"));
    assertEquals(
        Boolean.TRUE, response.getSuccess().getPermissionsMap().get("meander about asylum"));
  }

  @Test
  public void testServerPermissionsAndRoles() throws Exception {
    testServer
        .getDao()
        .createSubjectPermissions(
            "alfonso",
            Collections.singletonList("rescue dive"),
            Collections.singletonList("wreck diver"));
    testServer.getDao().createRolesPermissions("wreck diver", "saturation dive", "weld");

    AuthorizationResponse response =
        testClient
            .getClient()
            .authorize("alfonso", "saturation dive", "wreck dive", "rescue dive", "cave dive");

    assertTrue(response.hasSuccess());
    assertFalse(response.hasError());
    assertEquals(
        Sets.newHashSet("saturation dive", "wreck dive", "rescue dive", "cave dive"),
        response.getSuccess().getPermissionsMap().keySet());
    assertEquals(Boolean.FALSE, response.getSuccess().getPermissionsMap().get("cave dive"));
    assertEquals(Boolean.TRUE, response.getSuccess().getPermissionsMap().get("saturation dive"));
    assertEquals(Boolean.TRUE, response.getSuccess().getPermissionsMap().get("rescue dive"));
  }

  @Test
  public void testWildCardPermissions() throws Exception {
    testServer.getDao().createSubjectPermissions("jerry", "send:*", "receive:internal:*");

    AuthorizationResponse response =
        testClient
            .getClient()
            .authorize(
                "jerry",
                "send:internal:eric",
                "send:external:wanda",
                "receive:internal:bill",
                "receive:external:wanda");

    assertTrue(response.hasSuccess());
    assertFalse(response.hasError());
    assertEquals(
        Sets.newHashSet(
            "send:internal:eric",
            "send:external:wanda",
            "receive:internal:bill",
            "receive:external:wanda"),
        response.getSuccess().getPermissionsMap().keySet());
    assertEquals(Boolean.TRUE, response.getSuccess().getPermissionsMap().get("send:internal:eric"));
    assertEquals(
        Boolean.TRUE, response.getSuccess().getPermissionsMap().get("send:external:wanda"));
    assertEquals(
        Boolean.TRUE, response.getSuccess().getPermissionsMap().get("receive:internal:bill"));
    assertEquals(
        Boolean.FALSE, response.getSuccess().getPermissionsMap().get("receive:external:wanda"));
  }

  @Test
  public void testWildCardPermissionRoot() throws Exception {
    testServer.getDao().createSubjectPermissions("jerry", "send:*");

    AuthorizationResponse response = testClient.getClient().authorize("jerry", "send");

    assertTrue(response.hasSuccess());
    assertFalse(response.hasError());
    assertEquals(Sets.newHashSet("send"), response.getSuccess().getPermissionsMap().keySet());
    assertEquals(Boolean.TRUE, response.getSuccess().getPermissionsMap().get("send"));
  }

  @Test
  public void testAuthZQueryIsCached() throws Exception {
    // given permissions are set
    testServer.getDao().createSubjectPermissions("jerry", "send", "receive");
    {
      // when authz
      AuthorizationResponse response =
          testClient.getClient().authorize("jerry", "send", "receive", "foo", "bar");
      assertTrue(response.hasSuccess());
      assertEquals(Boolean.FALSE, response.getSuccess().getPermissionsMap().get("bar"));
    }
    // then the dao is re-queried only a single time
    verify(testServer.getDao(), times(1)).getSubjectAuthorization(eq("jerry"));

    // and given permissions are subsequently mutated
    testServer.getDao().addSubjectPermissions("jerry", "bar");
    {
      // when authz
      AuthorizationResponse response = testClient.getClient().authorize("jerry", "foo", "bar");
      assertTrue(response.hasSuccess());
      assertEquals(Boolean.TRUE, response.getSuccess().getPermissionsMap().get("bar"));
    }
    // then the dao is re-queried a second time (because of the mutation)
    verify(testServer.getDao(), times(2)).getSubjectAuthorization(eq("jerry"));
  }

  @Test
  public void testAuthZQueryCacheTTL() throws Exception {
    // given permissions are set
    testServer.getDao().createSubjectPermissions("jerry", "send", "receive");
    {
      // when authz
      AuthorizationResponse response =
          testClient.getClient().authorize("jerry", "send", "receive", "foo", "bar");
      assertTrue(response.hasSuccess());
      assertEquals(Boolean.FALSE, response.getSuccess().getPermissionsMap().get("bar"));
    }
    // then the dao is re-queried only a single time
    verify(testServer.getDao(), times(1)).getSubjectAuthorization(eq("jerry"));

    // and given the cache ttl expires
    testScheduler.advanceTimeBy(
        Constants.AUTHZ_DB_CACHE_TIME + 1, Constants.AUTHZ_DB_CACHE_TIME_UNIT);

    {
      // when authz
      AuthorizationResponse response = testClient.getClient().authorize("jerry", "foo", "bar");
      assertTrue(response.hasSuccess());
      assertEquals(Boolean.FALSE, response.getSuccess().getPermissionsMap().get("bar"));
    }
    // then the dao is re-queried a second time (because of the mutation)
    verify(testServer.getDao(), times(2)).getSubjectAuthorization(eq("jerry"));
  }
}
