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
import com.google.common.collect.Sets;
import com.nordstrom.gatekeeper.util.GatekeeperTestDao;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class GatekeeperDaoTest {

  @RegisterExtension static GatekeeperTestDao testDb = new GatekeeperTestDao();

  @Test
  void testCreateSubjectPermissions() {
    GatekeeperDao subject = testDb.getDao();

    // create some subjects with permissions
    subject.createSubjectPermissions("loopy louise", "jot on scratch pad", "meander about asylum");

    Authorization authorization = subject.getSubjectAuthorization("loopy louise");
    assertEquals(
        Sets.newHashSet("jot on scratch pad", "meander about asylum"),
        authorization.getStringPermissions());
    assertEquals(Sets.newHashSet(), authorization.getRoles());
  }

  @Test
  void testCreateRolePermissions() {
    GatekeeperDao subject = testDb.getDao();

    // create some permissions
    subject.createRolesPermissions(
        "psycho surgeon", "lobotomy", "anthroscopy", "biopsy", "circumcision");
    subject.createRolesPermissions("dentist", "root canals", "fillings", "cleanings", "crowns");
    subject.createRolesPermissions("wreck diver", "raise titanic", "treasure hunt");

    Set<String> rolePermissions = subject.getRolePermissions("psycho surgeon");
    assertEquals(
        Sets.newHashSet("lobotomy", "anthroscopy", "biopsy", "circumcision"), rolePermissions);
  }

  @Test
  void testCreateSubjectRoles() {
    GatekeeperDao subject = testDb.getDao();

    // create some subjects with roles
    subject.createSubjectRoles("alfonso", "wreck diver");
    subject.createSubjectRoles("polyglot jenny", "psycho_surgeon", "dentist", "wreck diver");
    subject.createSubjectRoles("boisterous bill", "dentist", "wreck diver");
    subject.createSubjectRoles("boring bob", "nobody");

    Authorization authorization = subject.getSubjectAuthorization("alfonso");
    assertEquals(Sets.newHashSet(), authorization.getPermissions());
    assertEquals(Sets.newHashSet("wreck diver"), authorization.getRoles());
  }

  @Test
  void testCreateSubjectPermissionsAndRoles() {
    GatekeeperDao subject = testDb.getDao();

    // create some subjects with roles and permissions
    subject.createSubjectPermissions(
        "long gone lassie",
        Lists.newArrayList("sit", "stay", "rescue"),
        Lists.newArrayList("wreck diver", "dentist", "psycho surgeon"));

    Authorization authorization = subject.getSubjectAuthorization("long gone lassie");
    assertEquals(Sets.newHashSet("sit", "stay", "rescue"), authorization.getPermissions());
    assertEquals(
        Sets.newHashSet("wreck diver", "dentist", "psycho surgeon"), authorization.getRoles());
  }

  @Test
  void testAddSubjectPermissions() {
    GatekeeperDao subject = testDb.getDao();

    // create some subjects with roles and permissions
    subject.createSubjectPermissions(
        "fantastic fred",
        Lists.newArrayList("eat peanuts"),
        Lists.newArrayList("dentist", "nobody"));

    Authorization authorization = subject.getSubjectAuthorization("fantastic fred");
    assertEquals(Sets.newHashSet("eat peanuts"), authorization.getPermissions());

    subject.addSubjectPermissions("fantastic fred", "new thing");
    authorization = subject.getSubjectAuthorization("fantastic fred");
    assertEquals(Sets.newHashSet("eat peanuts", "new thing"), authorization.getPermissions());
  }

  @Test
  void testAddSubjectRoles() {
    GatekeeperDao subject = testDb.getDao();

    // given a subject with a permission
    subject.createSubjectPermissions("fantastic fred", "eat peanuts");

    // when roles are added to the subject
    subject.addSubjectRoles("fantastic fred", "peanut roaster");

    // then the subject's authorization is correct
    Authorization authorization = subject.getSubjectAuthorization("fantastic fred");
    assertEquals(Sets.newHashSet("eat peanuts"), authorization.getPermissions());

    // and then the subject is authorized for the added role
    assertEquals(Sets.newHashSet("peanut roaster"), authorization.getRoles());
  }

  @Test
  void testRemoveSubjectRoles() {
    GatekeeperDao subject = testDb.getDao();

    // given a subject with a permission and role(s)
    subject.createSubjectPermissions(
        "fantastic fred",
        Lists.newArrayList("eat peanuts"),
        Lists.newArrayList("dentist", "nobody"));

    // when roles are added to the subject
    subject.removeSubjectRoles("fantastic fred", "nobody");

    // then the subject's authorization is correct
    Authorization authorization = subject.getSubjectAuthorization("fantastic fred");
    assertEquals(Sets.newHashSet("eat peanuts"), authorization.getPermissions());

    // and then the subject is NOT authorized for the removed role
    assertEquals(Sets.newHashSet("dentist"), authorization.getRoles());
  }

  @Test
  void testRemoveSubjectPermissions() {
    GatekeeperDao subject = testDb.getDao();

    subject.createSubjectPermissions("nobody", "nothing");
    subject.removeSubjectPermissions("nobody", "nothing");

    Authorization authorization = subject.getSubjectAuthorization("nobody");
    assertTrue(authorization.getRoles().isEmpty());
    assertTrue(authorization.getPermissions().isEmpty());
  }

  @Test
  void testCreateRolesPermissions() {
    GatekeeperDao subject = testDb.getDao();

    // create some roles
    subject.createRolesPermissions(
        "psycho surgeon", "lobotomy", "anthroscopy", "biopsy", "circumcision");
    subject.createRolesPermissions("wreck diver", "raise titanic", "treasure hunt");

    assertEquals(
        Sets.newHashSet("lobotomy", "anthroscopy", "biopsy", "circumcision"),
        subject.getRolePermissions("psycho surgeon"));
    assertEquals(
        Sets.newHashSet("raise titanic", "treasure hunt"),
        subject.getRolePermissions("wreck diver"));
    assertEquals(Sets.newHashSet(), subject.getRolePermissions("who"));
  }

  @Test
  void testAddRolePermissions() {
    GatekeeperDao subject = testDb.getDao();

    subject.createRolesPermissions("nobody", "nothing");
    subject.addRolePermissions("nobody", "void");
    assertEquals(Sets.newHashSet("nothing", "void"), subject.getRolePermissions("nobody"));
  }

  @Test
  void testRemoveRolePermissions() {
    GatekeeperDao subject = testDb.getDao();

    subject.createRolesPermissions("nobody", "nothing");
    assertEquals(Sets.newHashSet("nothing"), subject.getRolePermissions("nobody"));

    subject.removeRolePermissions("nobody", "nothing");
    assertEquals(Sets.newHashSet(), subject.getRolePermissions("nobody"));
  }
}
