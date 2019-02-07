import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.PatternLayout
import net.logstash.logback.encoder.LogstashEncoder

appender("CONSOLE", ConsoleAppender) {
  withJansi = true

  filter(ThresholdFilter) {
    level = DEBUG
  }
  encoder(LogstashEncoder)
}

logger("com.xjeffrose.xio.config.ConfigReloader", OFF)
logger("com.xjeffrose.xio.SSL.XioTrustManagerFactory", OFF)
logger("com.xjeffrose.xio.core.NullZkClient", OFF)
logger("io.netty.channel.DefaultChannelPipeline", DEBUG)
logger("io.netty.util.internal.NativeLibraryLoader", ERROR)
logger("io.netty.handler.ssl.CipherSuiteConverter", OFF)

if (System.getProperty("NFE_DEBUG") != null) {
  root(DEBUG, ["CONSOLE"])
} else {
  root(WARN, ["CONSOLE"])
}
