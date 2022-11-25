# Руководство по инсталляции mockingbird

Требования к окружению:
- mongodb версии 4.2 и выше. В целом mockingbird запустится и с 3.x, но как минимум не будут работать моки с pathPattern
- 512 MB памяти для контейнера (абсолютный минимум в районе 300 MB)

mockingbird доступен в двух вариантах
- native приложение

`ghcr.io/tinkoff/mockingbird:<TAG>-native`

Рекомендуемый образ для большинства случаев. Представляет собой скомпилированное в native-image с помощью GraalVM Scala приложение

- образ на классической JVM

`ghcr.io/tinkoff/mockingbird:<TAG>`

Рекомендуется только в случае необходимости использования mockingbird для нагрузочных тестов:
в отличие от нативного требует больше памяти, но позволяет использовать G1GC, что полезно в high-throughput сценариях

Для обоих вариантов: HTTP порт 8228, GRPC порт 9000

## mockingbird-native

При запуске образа нужно передать в CMD параметры. Типовой набор, с которого можно начать:

`-server -Xms256m -Xmx256m -XX:MaxDirectMemorySize=128m -Dconfig.file=/opt/mockingbird-native/qa.conf -Dlog.level=DEBUG -Dlog4j.formatMsgNoLookups=true`

Так-же необходимо примонтировать по пути `/opt/mockingbird-native/conf/secrets.conf` файл конфигурации mockingbird.
Минимальная конфигурация выглядит следующим образом:

```
{
  "secrets": {
    "mongodb": {
      "uri": "mongodb://.."
    },
    "security": {
      "secret": ".."
    }
  }
}
```
Подробнее о secrets.conf можно узнать из [руководства по настройке](configuration.md)

Логи приложение пишет в /opt/log/mockingbird-native

## mockingbird

Этот образ содержит приложение на классической JVM, поэтому параметры передаются через переменную окружения JAVA_OPTS.
Пример типовых настроек

`-server -XX:+AlwaysActAsServerClassMachine -Xms256m -Xmx256m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:ReservedCodeCacheSize=128m -XX:+PerfDisableSharedMem -Dconfig.resource=qa.conf -Dlog.level=DEBUG -Dlog4j.formatMsgNoLookups=true`

Так-же необходимо примонтировать по пути `/opt/mockingbird/conf/secrets.conf` файл конфигурации mockingbird.
Формат и содержимое `/opt/mockingbird/conf/secrets.conf` полностью идентичны оному для mockingbird-native

Логи приложение пишет в /opt/log/mockingbird