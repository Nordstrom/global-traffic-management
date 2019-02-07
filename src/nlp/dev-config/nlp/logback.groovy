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

logger("com.xjeffrose.xio.config.ConfigReloader", OFF)
logger("com.xjeffrose.xio.SSL.XioTrustManagerFactory", OFF)
logger("com.xjeffrose.xio.core.NullZkClient", OFF)
logger("io.netty.channel.DefaultChannelPipeline", OFF)
logger("io.netty.util.internal.NativeLibraryLoader", OFF)
logger("io.netty.handler.ssl.CipherSuiteConverter", OFF)

if (System.getProperty("DEBUG") != null) {
  root(DEBUG, ["CONSOLE"])
} else {
  root(WARN, ["CONSOLE"])
}
