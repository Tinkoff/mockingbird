package ru.tinkoff.tcb.mockingbird.dal2.model

import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.Scope

/**
 * Параметры для подбора заглушек подходящих под указанный путь.
 *
 * @param scope
 * @param path
 *   Путь который был передан в mockingbird при вызове заглушки. Подходящая у подходящей заглушки поле path будет в
 *   точности равно этому пути или переданный путь будет соотвествовать регулярному выражению хранимому в поле
 *   pathPattern.
 * @param method
 */
final case class StubMatchParams(scope: Scope, path: String, method: HttpMethod)
