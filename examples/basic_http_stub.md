# Базовые примеры работы с HTTP заглушками
## Persistent, ephemeral и countdown HTTP заглушки

Предполагается, что в mockingbird есть сервис `alpha`.

Создаем заглушку в скоупе `persistent`.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/handler1",
  "name": "Persistent HTTP Stub",
  "method": "GET",
  "scope": "persistent",
  "request": {
    "mode": "no_body",
    "headers": {}
  },
  "response": {
    "mode": "raw",
    "body": "persistent scope",
    "headers": {
      "Content-Type": "text/plain"
    },
    "code": "451"
  }
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "status" : "success",
  "id" : "29dfd29e-d684-462e-8676-94dbdd747e30"
}

```

Проверяем созданную заглушку.
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Ответ:
```
Код ответа: 451

Заголовки ответа:
Content-Type: 'text/plain'

Тело ответа:
persistent scope

```

Для этого же пути, создаем заглушку в скоупе `ephemeral`.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/handler1",
  "name": "Ephemeral HTTP Stub",
  "method": "GET",
  "scope": "ephemeral",
  "request": {
    "mode": "no_body",
    "headers": {}
  },
  "response": {
    "mode": "raw",
    "body": "ephemeral scope",
    "headers": {
      "Content-Type": "text/plain"
    },
    "code": "200"
  }
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "status" : "success",
  "id" : "13da7ef2-650e-4a54-9dca-377a1b1ca8b9"
}

```

И создаем заглушку в скоупе `countdown` с `times` равным 2.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/handler1",
  "times": 2,
  "name": "Countdown Stub",
  "method": "GET",
  "scope": "countdown",
  "request": {
    "mode": "no_body",
    "headers": {}
  },
  "response": {
    "mode": "raw",
    "body": "countdown scope",
    "headers": {
      "Content-Type": "text/plain"
    },
    "code": "429"
  }
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "status" : "success",
  "id" : "09ec1cb9-4ca0-4142-b796-b94a24d9df29"
}

```

Заданные заглушки отличаются возвращаемыми ответами, а именно содержимым `body` и `code`,
 в целом они могут быть как и полностью одинаковыми так и иметь больше различий.
 Скоупы заглушек в порядке убывания приоритета: Countdown, Ephemeral, Persistent

Так как заглушка `countdown` была создана с `times` равным двум, то следующие два
запроса вернут указанное в ней содержимое.
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Ответ:
```
Код ответа: 429

Заголовки ответа:
Content-Type: 'text/plain'

Тело ответа:
countdown scope

```
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Ответ:
```
Код ответа: 429

Заголовки ответа:
Content-Type: 'text/plain'

Тело ответа:
countdown scope

```

Последующие запросы будут возвращать содержимое заглушки `ephemeral`. Если бы её не было,
то вернулся бы ответ от заглушки `persistent`.
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Ответ:
```
Код ответа: 200

Заголовки ответа:
Content-Type: 'text/plain'

Тело ответа:
ephemeral scope

```

Чтобы получить теперь ответ от `persistent` заглушки нужно или дождаться, когда истекут
сутки с момента её создания или просто удалить `ephemeral` заглушку.
```
curl \
  --request DELETE \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub/13da7ef2-650e-4a54-9dca-377a1b1ca8b9' \
  --header 'Content-Type: application/json'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "status" : "success",
  "id" : null
}

```

После удаления `ephemeral` заглушки, при запросе вернется результат заглушки `persistent`
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Ответ:
```
Код ответа: 451

Заголовки ответа:
Content-Type: 'text/plain'

Тело ответа:
persistent scope

```
## Использование параметров пути в HTTP заглушках

Заглушка может выбираться в том числе и на основании регулярного выражения
в пути, это может быть не очень эффективно с точки зрения поиска такой заглушки.
Поэтому без необходимости, лучше не использовать этот механизм.

Предполагается, что в mockingbird есть сервис `alpha`.

Скоуп в котором создаются заглушки не важен. В целом скоуп влияет только
на приоритет заглушек. В данном случае заглушка создается в скоупе `countdown`.
В отличие от предыдущих примеров, здесь для указания пути для срабатывания
заглушки используется поле `pathPattern`, вместо `path`. Так же, ответ который
формирует заглушка не статичный, а зависит от параметров пути.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "pathPattern": "/alpha/handler2/(?<obj>[-_A-z0-9]+)/(?<id>[0-9]+)",
  "times": 2,
  "name": "Simple HTTP Stub with path pattern",
  "method": "GET",
  "scope": "countdown",
  "request": {
    "mode": "no_body",
    "headers": {}
  },
  "response": {
    "mode": "json",
    "body": {
      "static_field": "Fixed part of reponse",
      "obj": "${pathParts.obj}",
      "id": "${pathParts.id}"
    },
    "headers": {
      "Content-Type": "application/json"
    },
    "code": "200"
  }
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "status" : "success",
  "id" : "c8c9d92f-192e-4fe3-8a09-4c9b69802603"
}

```

Теперь сделаем несколько запросов, который приведут к срабатыванию этой заглшки,
чтобы увидеть, что результат действительно зависит от пути.
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler2/alpha/123'

```

Ответ:
```
Код ответа: 200

Заголовки ответа:
Content-Type: 'application/json'

Тело ответа:
{
  "static_field" : "Fixed part of reponse",
  "obj" : "alpha",
  "id" : "123"
}

```
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler2/beta/876'

```

Ответ:
```
Код ответа: 200

Заголовки ответа:
Content-Type: 'application/json'

Тело ответа:
{
  "static_field" : "Fixed part of reponse",
  "obj" : "beta",
  "id" : "876"
}

```
