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
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

@Slf4j
@RequiredArgsConstructor
public class GatekeeperDbRealm extends AuthorizingRealm {

  private final SubjectTokenRealm subjectTokenRealm;
  private final GatekeeperDao gatekeeperDao;

  @Override
  public Class getAuthenticationTokenClass() {
    return AuthToken.class;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    if (principals.getPrimaryPrincipal() instanceof String) {
      String subject = (String) principals.getPrimaryPrincipal();
      return gatekeeperDao.getSubjectAuthorization(subject);
    } else {
      log.warn("using empty auth info - this should NOT be happening");
      return EmptyAuthInfo.INSTANCE;
    }
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException {
    return subjectTokenRealm.getAuthenticationInfo(token);
  }

  private enum EmptyAuthInfo implements AuthorizationInfo {
    INSTANCE;

    @Override
    public Collection<String> getRoles() {
      return Collections.emptyList();
    }

    @Override
    public Collection<String> getStringPermissions() {
      return Collections.emptyList();
    }

    @Override
    public Collection<Permission> getObjectPermissions() {
      return Collections.emptyList();
    }
  }
}
