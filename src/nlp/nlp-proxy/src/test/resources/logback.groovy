/*
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
import ch.qos.logback.classic.filter.ThresholdFilter

appender("CONSOLE", ConsoleAppender) {
  withJansi = true

  filter(ThresholdFilter) {
    level = DEBUG
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%-4relative [%thread] %-5level %logger{30} - %msg%n"
    outputPatternAsHeader = false
  }
}

appender("DEVNULL", FileAppender) {
  file = "/dev/null"
  filter(ThresholdFilter) {
    level = DEBUG
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%-4relative [%thread] %-5level %logger{30} - %msg%n"
    outputPatternAsHeader = false
  }
}

logger("com.xjeffrose.xio.config.ConfigReloader", OFF)
logger("com.xjeffrose.xio.SSL.XioTrustManagerFactory", OFF)
logger("com.xjeffrose.xio.core.NullZkClient", OFF)
logger("io.netty.channel.DefaultChannelPipeline", DEBUG)
logger("io.netty.util.internal.NativeLibraryLoader", ERROR)
logger("io.netty.handler.ssl.CipherSuiteConverter", OFF)

if (System.getProperty("DEBUG") != null) {
  root(DEBUG, ["CONSOLE"])
} else if (System.getProperty("COVERAGE") != null) {
  root(DEBUG, ["DEVNULL"])
} else {
  root(WARN, ["CONSOLE"])
}
