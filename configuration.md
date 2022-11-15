# Конфигурация mockingbird

Mockingbird конфигурируется посредством файла secrets.conf, имеющего следующий вид:

```
{
  "secrets": {
    "server": {
      "allowedOrigins": [
        "http://localhost",
        "http://localhost:3000",
        ...
      ]
    },
    "security": {
      "secret": ".."
    },
    "mongodb": {
      "uri": "mongodb://.."
    },
    "proxy": {
      "excludedRequestHeaders": [..],
      "excludedResponseHeaders": [..],
      "proxyServer": {
        "type": "http" | "socks",
        "type": "..",
        "port": "..",
        "nonProxy": ["..", ...],
        "onlyProxy": ["..", ...],
        "auth": {
          "user": "..",
          "password": ".."
        }
      }
    }
  }
}
```

### Секция server

Здесь указыватся ориджены для CORS. Эти настройки влияют на работоспособность UI Mockingbird, а также swagger-ui

### Секция security

Обязательная секция. Здесь указывается secret - ключ шифрования для конфигураций source и destination.
Рекомендуется использовать достаточно длинный ключ (от 40 символов)

### Секция mongodb

Обязательная секция. Здесь указывается uri для подключения к mongodb, которую будет использовать mockingbird.
Здесь же можно переопределить названия коллекций, которые будет создавать mockingbird (в примере перечислены все возможные поля со значениями по-умолчанию, не обязательно указывать все):

```
{
  "secrets": {
    "mongodb": {
      "uri": "mongodb://..",
      "collections": {
        "stub": "mockingbirdStubs",
        "state": "mockingbirdStates",
        "scenario": "mockingbirdScenarios",
        "service": "mockingbirdServices",
        "label": "mockingbirdLabels",
        "grpcStub": "mockingbirdGrpcStubs"
      }
    }
  }
}
```

### Секция proxy

В данной секции можно указать заголовки, которые mockingbird будет отбрасывать при работе в режимах proxy и json-proxy

Пример типовой конфигурации:

```
{
  "secrets": {
    "proxy": {
      "excludedRequestHeaders": ["Host", "HOST", "User-Agent", "user-agent"],
      "excludedResponseHeaders": ["transfer-encoding"]
    }
  }
}
```

Так-же в этой секции можно указать настройки прокси сервера. Эти настройки влияют на ВСЕ http запросы, которые делаем mockingbird, т.е.:
- запросы к внешнему серверу с proxy моках
- запросы в source и destination (включая init/shutdown)

Назначения полей:
- type - тип прокси сервера
- host - хост
- port - порт
- nonProxy - (опционально) перечень доменов (масок доменов), запросы к которым НЕ НУЖНО проксировать
- onlyProxy - (опционально) перечень доменов (масок доменов), запросы к которым НУЖНО проксировать.
Если указать одновременно nonProxy и onlyProxy, то nonProxy будет иметь приоритет
- auth - (опционально) параметры авторизации

Можно указывать как домены, так и маски: "localhost", "*.local", "127.*"