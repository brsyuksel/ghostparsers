package paytrek.ghostparsers.jobq

import java.util.UUID

import io.circe.Json

case class JobRequest(body: Json)
case class Job(id: UUID, jobRequest: JobRequest)

sealed trait Status
object Status {
  case object Pending    extends Status
  case object Processing extends Status
  case object Completed  extends Status
}

case class JobStatus(id: UUID,
                     status: Status,
                     createdAt: Long,
                     updatedAt: Long,
                     result: Json = Json.Null,
                     completedAt: Option[Long] = None)
