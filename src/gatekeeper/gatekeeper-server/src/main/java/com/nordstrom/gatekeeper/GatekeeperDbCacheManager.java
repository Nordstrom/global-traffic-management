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

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.MapCache;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.SoftHashMap;

/** https://shiro.apache.org/caching.html */
@Slf4j
public class GatekeeperDbCacheManager extends AbstractCacheManager {

  private final SoftHashMap<PrincipalCollection, Authorization> authorizationCache =
      new SoftHashMap<>();

  @Override
  protected Cache createCache(String name) throws CacheException {
    /**
     * Note: {@link org.apache.shiro.realm.AuthorizingRealm} uses a static INSTANCE_COUNT which gets
     * incremented in tests. However in reality there is only 1 authorizing realm and 1 cache.
     */
    if (name.startsWith("com.nordstrom.gatekeeper.GatekeeperDbRealm.authorizationCache")) {
      return new TTLMapCache<>(
          name,
          authorizationCache,
          Constants.AUTHZ_DB_CACHE_TIME,
          Constants.AUTHZ_DB_CACHE_TIME_UNIT);
    } else {
      return new MapCache<>(name, new SoftHashMap<>());
    }
  }

  public void bustAuthorizationCache() {
    log.debug("clearing db cache");
    authorizationCache.clear();
  }
}
