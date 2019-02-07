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

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.MapCache;

@Slf4j
public class TTLMapCache<K, V> extends MapCache<K, V> {
  private final long timeValue;
  private final TimeUnit timeUnit;

  private final ConcurrentHashMap<Object, Disposable> disposables = new ConcurrentHashMap<>();

  /**
   * @param name the name of the cache.
   * @param concurrentMap a <b>thread safe</b> map containing the items to cache.
   * @param timeValue how long to cache items.
   * @param timeUnit the time unit of how long to cache items.
   */
  public TTLMapCache(String name, Map<K, V> concurrentMap, long timeValue, TimeUnit timeUnit) {
    super(name, concurrentMap);
    this.timeValue = timeValue;
    this.timeUnit = timeUnit;
  }

  private void scheduleRemoval(K key) {
    clearRemovalSchedule(key);
    disposables.put(
        key,
        Single.timer(timeValue, timeUnit, Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(
                (t) -> remove(key),
                (error) -> {
                  log.error("unexpected timer error", error);
                  remove(key);
                }));
  }

  private void clearRemovalSchedule(K key) {
    Disposable previous = disposables.remove(key);
    if (previous != null && !previous.isDisposed()) {
      previous.dispose();
    }
  }

  @Override
  public V get(K key) throws CacheException {
    return super.get(key);
  }

  @Override
  public V put(K key, V value) throws CacheException {
    scheduleRemoval(key);
    return super.put(key, value);
  }

  @Override
  public V remove(K key) throws CacheException {
    clearRemovalSchedule(key);
    return super.remove(key);
  }

  @Override
  public void clear() throws CacheException {
    keys().forEach(this::clearRemovalSchedule);
    super.clear();
  }
}
