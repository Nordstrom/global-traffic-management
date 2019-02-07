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
package com.nordstrom.keymaster;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class Main {
  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length != 1) {
      fatalError();
    }
    int port = Integer.parseInt(args[0]);
    System.out.println(String.format("starting service with port: %d", port));

    KeymasterServiceLocator serviceLocator = new KeymasterServiceLocator();
    KeymasterServer keymasterServer = serviceLocator.getKeymasterServer();
    keymasterServer.start(port);
    keymasterServer.blockUntilShutdown();
  }

  private static void fatalError() {
    throw new RuntimeException("please specify: '<port>'");
  }
}
