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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;

@ParametersAreNonnullByDefault
public class Authorizer {

  private final DefaultSecurityManager defaultSecurityManager;

  public Authorizer(DefaultSecurityManager defaultSecurityManager) {
    this.defaultSecurityManager = defaultSecurityManager;
  }

  Subject authWithSubjectToken(String subjectId) {
    Subject subject = new Subject.Builder(defaultSecurityManager).buildSubject();
    subject.login(new AuthToken(subjectId));
    return subject;
  }

  Optional<Map<String, Boolean>> authorize(String subjectId, List<String> permissionsRequested) {
    Subject subject = authWithSubjectToken(subjectId);
    if (subject.isAuthenticated()) {
      return Optional.of(
          permissionsRequested.stream().collect(Collectors.toMap(p -> p, subject::isPermitted)));
    } else {
      return Optional.empty();
    }
  }
}
