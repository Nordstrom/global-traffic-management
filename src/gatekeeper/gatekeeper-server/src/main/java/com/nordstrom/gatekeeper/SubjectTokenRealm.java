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

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.Realm;

public class SubjectTokenRealm implements Realm {

  @Override
  public String getName() {
    return "subject-token-realm";
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return token.getPrincipal() instanceof String;
  }

  @Override
  public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException {
    if (token.getPrincipal() instanceof String) {
      return new Authentication((String) token.getPrincipal());
    }
    throw new AuthenticationException("invalid principal");
  }
}
