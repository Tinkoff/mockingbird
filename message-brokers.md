# Работа с очередями

Mockingbird взаимодействует с брокерами сообщений через HTTP API, благодаря чему теоретически поддерживаются любые возможные MQ.
На практике некоторе брокеры требуют установки дополнительных sidecar-коробок, как, например, HTTP-Bridge для WebsphereMQ или rest-proxy для Kafka.
Здесь мы рассмотрим примеры настройки mockingbird для работы с различными MQ.
Примеры ниже являются шаблонами, по которым можно самостоятельно настроить Mockingbird через UI

## RabbitMQ

RabbitMQ имеет встроенный rest API, благодаря чему установка доплнительных решений не требуется.

Официальная документация: https://www.rabbitmq.com/management.html#http-api

Пример конфигурации source (mockingbird читает из очереди):
```
Запрос:

{
  "body": "{\"count\":1,\"ackmode\":\"ack_requeue_false\",\"encoding\":\"auto\"}",
  "headers": {
    "Authorization": "Basic xxxxxxx"
  },
  "jenumerate": "$",
  "jextract": "payload",
  "jstringdecode": true,
  "method": "POST",
  "url": "http://<rabbitmq_host>:15672/api/queues/<virtual_host>/<queue>/get"
}
```

Назначение большинства полей понятно из названий, но назначение некоторых поля стоит раскрыть подробно:
- `jenumerate` - наличие этого поля означает, что в ответе метода может быть несколько сообщений, значение представляет собой путь до поля с массивом. В данном случае массив находится непосредственно в корне ответа
- `jextract` - путь до содержимого сообщения в ответе. В данном случае это поле `payload`
- `jstringdecode` - признак того, что сообщение является json-строкой, в которой содержится экранированый JSON. При установке jstringdecode в true этот JSON будет распаршен

Использование этих трёх полей имеет смысл только для API, возвращающих JSON и может в ином случае приводить к ошибкам

Пример конфигурации destination (mockingbird пишет в очередь):
```
Запрос:

{
  "body": {
    "payload": "${_message}", // сюда подставляется ответ, который сформировал мок
    "payload_encoding": "string",
    "properties": {},
    "routing_key": "<routing_key>"
  },
  "headers": {
    "Authorization": "Basic xxxxxxx"
  },
  "method": "POST",
  "stringifybody": true,
  "url": "http://<rabbitmq_host>:15672/api/exchanges/<virtual_host>/<exchange>/publish"
}
```

Назначение большинства полей понятно из названий, но назначение некоторых поля стоит раскрыть подробно:
- `stringifybody` - означает, что ответ, сформированый моком, нужно заэкранировать и передать в шаблонизатор как JSON строку

## WebsphereMQ

Для работы с WebsphereMQ требуется установка [IBM MQ bridge for HTTP](https://www.ibm.com/docs/en/ibm-mq/8.0?topic=mq-bridge-http)

Пример конфигурации source (mockingbird читает из очереди):
```
Запрос:

{
  "bypassCodes": [504],
  "headers": {
    "Authorization": "Basic xxxxxxx"
  },
  "method": "DELETE",
  "url": "http://<http_bridge_host>:8080/WMQHTTP2/msg/queue/<queue>/"
}
```

Назначение большинства полей понятно из названий, но назначение некоторых поля стоит раскрыть подробно:
- `bypassCodes` - коды ответа сервера, которые не следует считать ошибочными. В данном случае 504 это признак отсутсвия сообщений, что является нормой

Пример конфигурации destination (mockingbird пишет в очередь):
```
Запрос:

{
  "headers": {
    "Authorization": "Basic xxxxxxx",
    "Content-Type": "text/xml",
    "x-msg-class": "TEXT"
  },
  "method": "POST",
  "url": "http://<http_bridge_host>:8080/WMQHTTP2/msg/queue/<queue>/"
}
```

## Kafka

Для работы с Kafka требуется установить и настроить [Kafka REST Proxy](https://github.com/confluentinc/kafka-rest)

Чтение из топиков кафки с помощью Kafka REST Proxy требует дополнительного созданию (и удаления) консьюмеров и подписок, для этого предусмотрены блоки Init и Shutdown.

`<consumer_name>` и `<consumer_instance_name>` - произвольные уникальные в рамках конфига имена

Пример конфигурации source (mockingbird читает JSON из топика):
```
Запрос:

{
  "headers": {
    "Accept": "application/vnd.kafka.json.v2+json"
  },
  "jenumerate": "$",
  "jextract": "value",
  "method": "GET",
  "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>/records"
}

Init: 

[
  {
    "body": "{\"name\": \"<consumer_instance_name>\", \"format\": \"json\", \"auto.offset.reset\": \"earliest\"}",
    "headers": {
      "Content-Type": "application/vnd.kafka.v2+json"
    },
    "method": "POST",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>"
  },
  {
    "body": "{\"topics\":[\"<topic>\"]}",
    "headers": {
      "Content-Type": "application/vnd.kafka.v2+json"
    },
    "method": "POST",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>/subscription"
  }
]

Shutdown:

[
  {
    "method": "DELETE",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>"
  }
]

ReInit triggers:

[
  {
    "mode": "json",
    "code": 404,
    "body": {"error_code":40403,"message":"Consumer instance not found."}
  }
]
```

Пример конфигурации source (mockingbird читает Avro из топика):
```
Запрос:

{
  "headers": {
    "Accept": "application/vnd.kafka.avro.v2+json"
  },
  "jenumerate": "$",
  "jextract": "value",
  "method": "GET",
  "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>/records"
}

Init:

[
  {
    "body": "{\"name\": \"<consumer_instance_name>\", \"format\": \"avro\", \"auto.offset.reset\": \"earliest\"}",
    "headers": {
      "Content-Type": "application/vnd.kafka.v2+json"
    },
    "method": "POST",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>"
  },
  {
    "body": "{\"topics\":[\"<topic>\"]}",
    "headers": {
      "Content-Type": "application/vnd.kafka.v2+json"
    },
    "method": "POST",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>/subscription"
  }
]

Shutdown:

[
  {
    "method": "DELETE",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>"
  }
]

ReInit triggers:

[
  {
    "mode": "json",
    "code": 404,
    "body": {"error_code":40403,"message":"Consumer instance not found."}
  }
]
```

Назначение большинства полей понятно из названий, но назначение некоторых поля стоит раскрыть подробно:
- `jenumerate` - наличие этого поля означает, что в ответе метода может быть несколько сообщений, значение представляет собой путь до поля с массивом. В данном случае массив находится непосредственно в корне ответа 
- `jextract` - путь до содержимого сообщения в ответе. В данном случае это поле `value`

kafka-rest-proxy на данный момент (май 2022) [не поддерживает](https://github.com/confluentinc/kafka-rest/issues/620) топики, в которых сообщение сериализуется в Avro, а ключ - нет

Пример конфигурации destination (mockingbird пишет JSON в топик):
```
Запрос:

{
  "body": {
    "records": [
      {
        "value": "${_message}" //сюда подставляется ответ, который сформировал мок
      }
    ]
  },
  "headers": {
    "Content-Type": "application/vnd.kafka.json.v2+json"
  },
  "method": "POST",
  "url": "http://<kafka_rest_proxy_host>/topics/<topic>"
}
```

Пример конфигурации destination (mockingbird пишет Avro в топик):
```
Запрос:

{
  "body": {
    "key_schema_id": <id схемы ключа из registry (целое число)>,
    "records": [
      {
        "key": "${_message.key}",
        "value": "${_message.value}"
      }
    ],
    "value_schema_id": <id схемы значения из registry (целое число)>
  },
  "headers": {
    "Content-Type": "application/vnd.kafka.avro.v2+json"
  },
  "method": "POST",
  "url": "http://<kafka_rest_proxy_host>/topics/<topic>"
}
```

Дополнительные пояснения:
данный пример предполагает, что ответ мока выглядит следующим образом:
```
{
  "key": <содержимое ключа>,
  "value": <содержимое сообщения>
}
```

kafka-rest-proxy на данный момент (май 2022) [не поддерживает](https://github.com/confluentinc/kafka-rest/issues/620) топики, в которых сообщение сериализуется в Avro, а ключ - нет