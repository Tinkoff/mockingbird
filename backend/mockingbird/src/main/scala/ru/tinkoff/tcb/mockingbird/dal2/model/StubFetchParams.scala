package ru.tinkoff.tcb.mockingbird.dal2.model

import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.numeric.PosInt

/**
 * Параметры для отбора заглушек для отображения их списка в UI.
 *
 * @param page
 *   номер страницы для которой формируется список заглушек
 * @param query
 *   строка запроса, рассматривается как точный ID заглушки или используется как регулярное выражения и сопоставляется с
 *   полями name, path, pathPattern
 * @param service
 *   имя сервиса к которому относится заглушка (поле serviceSuffix)
 * @param labels
 *   список лейблов, которыми должна быть отмечена заглушка, все перечисленные лейблы должны содержаться в поле labels
 *   заглушки, хранящейся в хранилище
 * @param count
 *   количество заглушек, отображаемых на странице
 */
final case class StubFetchParams(
    page: NonNegInt,
    query: Option[String],
    service: Option[String],
    labels: Seq[String],
    count: PosInt
)
