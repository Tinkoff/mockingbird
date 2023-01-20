package ru.tinkoff.tcb.mockingbird.config

import scala.concurrent.duration.FiniteDuration

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus.*
import net.ceedubs.ficus.readers.ArbitraryTypeReader.*
import net.ceedubs.ficus.readers.EnumerationReader.*

case class JsSandboxConfig(allowedClasses: Set[String] = Set())

case class ServerConfig(
    interface: String,
    port: Int,
    allowedOrigins: Seq[String],
    healthCheckRoute: Option[String],
    sandbox: JsSandboxConfig
)

case class SecurityConfig(secret: String)

case class ProxyServerAuth(user: String, password: String)

object ProxyServerType extends Enumeration {
  val Http  = Value("http")
  val Socks = Value("socks")
}

case class ProxyServerConfig(
    `type`: ProxyServerType.Value,
    host: String,
    port: Int,
    nonProxy: Seq[String] = Seq(),
    onlyProxy: Seq[String] = Seq(),
    auth: Option[ProxyServerAuth]
)

case class ProxyConfig(
    excludedRequestHeaders: Seq[String],
    excludedResponseHeaders: Set[String],
    proxyServer: Option[ProxyServerConfig],
    insecureHosts: Seq[String]
)

case class EventConfig(fetchInterval: FiniteDuration, reloadInterval: FiniteDuration)

case class MongoConfig(uri: String, collections: MongoCollections)

case class MongoCollections(
    stub: String,
    state: String,
    scenario: String,
    service: String,
    label: String,
    grpcStub: String,
    source: String,
    destination: String
)

case class MockingbirdConfiguration(
    server: ServerConfig,
    security: SecurityConfig,
    mongo: MongoConfig,
    proxy: ProxyConfig,
    event: EventConfig
)

object MockingbirdConfiguration {
  def load(): MockingbirdConfiguration =
    load(ConfigFactory.load().getConfig("ru.tinkoff.tcb"))

  def load(config: Config): MockingbirdConfiguration =
    MockingbirdConfiguration(
      config.as[ServerConfig]("server"),
      config.as[SecurityConfig]("security"),
      config.as[MongoConfig]("db.mongo"),
      config.as[ProxyConfig]("proxy"),
      config.as[EventConfig]("event")
    )

  private lazy val conf = load()

  val server: ULayer[ServerConfig]     = ZLayer.succeed(conf.server)
  val security: ULayer[SecurityConfig] = ZLayer.succeed(conf.security)
  val mongo: ULayer[MongoConfig]       = ZLayer.succeed(conf.mongo)
  val proxy: ULayer[ProxyConfig]       = ZLayer.succeed(conf.proxy)
  val event: ULayer[EventConfig]       = ZLayer.succeed(conf.event)
}
