package ru.tinkoff.tcb.utils.circe

import io.circe.Json

package object optics {
  val JLens = new JsonOptic(Seq.empty)

  private[optics] def construct(part: PathPart, value: Json): Json =
    part match {
      case Field(name)  => Json.obj(name -> value)
      case Index(index) => Json.fromValues(Vector.fill(index)(Json.Null) :+ value)
      case Traverse     => Json.arr(value)
    }

  private[optics] def modifyPart(part: PathPart): (Option[Json] => Json) => Json => Json = { mod =>
    part match {
      case Field(name) =>
        json =>
          json.asObject.fold(construct(part, mod(None)))(jo =>
            Json.fromJsonObject(jo.add(name, jo(name).fold(mod(None))(j => mod(Some(j)))))
          )
      case Index(index) =>
        json =>
          json.asArray
            .map { ja =>
              if (ja.length > index) Json.fromValues(ja.updated(index, mod(Some(ja(index)))))
              else {
                val itemsToAdd = index - ja.length + 1
                Json.fromValues(
                  ja ++ Vector.tabulate(itemsToAdd)(idx => if (idx == itemsToAdd - 1) mod(None) else Json.Null)
                )
              }
            }
            .getOrElse(construct(part, mod(None)))
      case Traverse =>
        json =>
          json.asArray.map(arr => Json.fromValues(arr.map(j => mod(Some(j))))).getOrElse(construct(part, mod(None)))
    }
  }

  private[optics] def modifyPart(default: => Json)(part: PathPart): (Json => Json) => Json => Json = mod =>
    modifyPart(part) {
      case Some(value) => mod(value)
      case None        => mod(default)
    }

  private[optics] def verifyPart(part: PathPart): Json => Boolean = { json =>
    part match {
      case Field(name)  => json.asObject.exists(_.contains(name))
      case Index(index) => json.asArray.exists(_.length > index)
      case Traverse     => json.isArray
    }
  }
}
