package paytrek.ghostparsers

import java.util.UUID

import zio._
import zio.clock.Clock
import io.circe.Json
import nequi.zio.logger.Logger

package object jobq extends JobQ.Service[JobQ with Clock with Logger] {
  override def enqueue(jReq: JobRequest): ZIO[JobQ with Clock with Logger, Throwable, JobStatus] =
    ZIO.accessM(_.jobq.enqueue(jReq))

  override def dequeue: ZIO[JobQ with Clock with Logger, Throwable, Job] =
    ZIO.accessM(_.jobq.dequeue)

  override def getStatus(id: UUID): ZIO[JobQ with Clock with Logger, Throwable, Option[JobStatus]] =
    ZIO.accessM(_.jobq.getStatus(id))

  override def complete(id: UUID, result: Json): ZIO[JobQ with Clock with Logger, Throwable, Option[JobStatus]] =
    ZIO.accessM(_.jobq.complete(id, result))

  override def clean: ZIO[JobQ with Clock with Logger, Throwable, Unit] =
    ZIO.accessM(_.jobq.clean)

  override def queued: ZIO[JobQ with Clock with Logger, Throwable, Int] =
    ZIO.accessM(_.jobq.queued)
}
