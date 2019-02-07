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

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermission;

@RequiredArgsConstructor
public class RoleResolver implements RolePermissionResolver {

  private final GatekeeperDao gatekeeperDao;

  @Override
  public Collection<Permission> resolvePermissionsInRole(String roleId) {
    return gatekeeperDao
        .getRolePermissions(roleId)
        .stream()
        .map(WildcardPermission::new)
        .collect(Collectors.toList());
  }
}
