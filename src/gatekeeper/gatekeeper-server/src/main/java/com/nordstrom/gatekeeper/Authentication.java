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

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

@ToString
@RequiredArgsConstructor
public class Authentication implements AuthenticationInfo {
  private final String subjectId;

  private PrincipalCollection principals;

  @Override
  public PrincipalCollection getPrincipals() {
    if (principals == null) {
      principals = new SimplePrincipalCollection(subjectId, "subject-token-realm");
    }
    return principals;
  }

  @Override
  public Object getCredentials() {
    return subjectId;
  }
}
