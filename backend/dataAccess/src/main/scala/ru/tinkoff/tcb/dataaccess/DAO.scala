package ru.tinkoff.tcb.dataaccess

import cats.tagless.autoFunctorK

import ru.tinkoff.tcb.generic.RootOptionFields

@autoFunctorK
trait DAO[F[_], T] {
  type Query
  type Patch
  type Sort

  def findOne(query: Query): F[Option[T]]

  def findChunk(query: Query, offset: Int, size: Int, sort: Sort*): F[Vector[T]]

  def insert(t: T): F[Int]

  def insertMany(ts: Seq[T]): F[Int]

  def update(query: Query, patches: Patch*): F[UpdateResult]

  def update(query: Query, patches: Iterable[Patch]): F[UpdateResult]

  def update(entity: T)(implicit rof: RootOptionFields[T]): F[UpdateResult]

  def updateIf(query: Query, entity: T)(implicit rof: RootOptionFields[T]): F[UpdateResult]

  def delete(query: Query): F[Long]
}
