package ru.tinkoff.tcb.mockingbird.dal2.model

import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.Scope

/**
 * Параметры для поиска заглушек.
 *
 * @param scope
 * @param pathPattern
 *   представляет собой или строку, которая соответствует точному пути, или регулярное выражение. Заглушка может
 *   содержать или одно, или другое.
 * @param method
 */
final case class StubFindParams(scope: Scope, path: StubPath, method: HttpMethod)
