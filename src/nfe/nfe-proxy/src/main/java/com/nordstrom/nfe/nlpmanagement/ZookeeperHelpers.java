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
package com.nordstrom.nfe.nlpmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjeffrose.xio.core.ZkClient;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ZookeeperHelpers {
  public static List<String> safeGetChildren(ZkClient zkClient, String path) {
    List<String> children = zkClient.getChildren(path);
    return children != null ? children : Collections.emptyList();
  }

  public static <T> Optional<T> infoAtZookeeperPath(
      ZkClient zkClient, ObjectMapper objectMapper, String zkPath, Class<T> tClass) {
    try {
      String jsonString = zkClient.get(zkPath);
      return Optional.of(objectMapper.readValue(jsonString, tClass));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public static String pathFromComponents(String... components) {
    return String.join("/", components);
  }
}
