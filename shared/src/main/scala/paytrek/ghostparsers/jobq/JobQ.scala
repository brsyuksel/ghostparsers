package paytrek.ghostparsers.jobq

import java.util.UUID

import zio._
import zio.clock.Clock
import io.circe.Json
import nequi.zio.logger.Logger

trait JobQ {
  val jobq: JobQ.Service[Clock with Logger]
}

object JobQ {
  trait Service[R] {
    def enqueue(j: JobRequest): ZIO[R, Throwable, JobStatus]
    def dequeue: ZIO[R, Throwable, Job]
    def getStatus(id: UUID): ZIO[R, Throwable, Option[JobStatus]]
    def complete(id: UUID, result: Json): ZIO[R, Throwable, Option[JobStatus]]
    def clean: ZIO[R, Throwable, Unit]
    def queued: ZIO[R, Throwable, Int]
  }
}
