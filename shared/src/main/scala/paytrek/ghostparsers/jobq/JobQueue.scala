package paytrek.ghostparsers.jobq

import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.collection.immutable.TreeMap

import zio._
import zio.clock
import zio.clock.Clock
import zio.stm._
import io.circe.Json
import nequi.zio.logger._

final class JobQueue private (Q: TQueue[Job], S: TRef[TreeMap[String, JobStatus]], TTL: Long)
    extends JobQ.Service[Clock with Logger] {
  private def updateStatus(t: TreeMap[String, JobStatus], s: JobStatus) =
    t ++ TreeMap(s.id.toString -> s)

  override def enqueue(jReq: JobRequest): ZIO[Clock with Logger, Throwable, JobStatus] =
    for {
      uuid <- ZIO.effect(UUID.randomUUID)
      job = Job(id = uuid, jReq)
      _   <- Q.offer(job).commit
      now <- clock.currentTime(TimeUnit.MILLISECONDS)
      st = JobStatus(id = uuid, status = Status.Pending, createdAt = now, updatedAt = now)
      _ <- S.update(updateStatus(_, st)).commit
      _ <- info(s"job has been queued, uuid: ${uuid.toString}")
    } yield st

  override def dequeue: ZIO[Clock with Logger, Throwable, Job] =
    for {
      job <- Q.take.commit
      now <- clock.currentTime(TimeUnit.MILLISECONDS)
      cSt <- S.get.commit.map(_.getOrElse(job.id.toString, JobStatus(job.id, Status.Processing, now, now)))
      st = cSt.copy(status = Status.Processing, updatedAt = now)
      _ <- S.update(updateStatus(_, st)).commit
      _ <- info(s"job has been taken, uuid: ${job.id.toString}")
    } yield job

  override def getStatus(id: UUID): ZIO[Clock with Logger, Throwable, Option[JobStatus]] =
    S.get.commit.map(_.get(id.toString))

  override def complete(id: UUID, result: Json): ZIO[Clock with Logger, Throwable, Option[JobStatus]] =
    S.get.commit
      .flatMap(m => ZIO.fromOption(m.get(id.toString)))
      .foldM(
        { _ =>
          Task.effect(None)
        }, { j =>
          for {
            now <- clock.currentTime(TimeUnit.MILLISECONDS)
            st = j
              .copy(updatedAt = now, completedAt = Some(now), result = result, status = Status.Completed)
            _ <- S.update(updateStatus(_, st)).commit
            _ <- info(s"job has been completed, uuid: ${id.toString}")
          } yield Some(st)
        }
      )

  override def clean: ZIO[Clock with Logger, Throwable, Unit] =
    for {
      now <- clock.currentTime(TimeUnit.MILLISECONDS)
      t   <- S.get.commit
      expireds = t.filter(_._2.completedAt.exists(_ >= now + TTL)).keys.toList
      updated  = expireds.foldLeft(t)(_ - _)
      _ <- S.set(updated).commit
      _ <- info(s"completed ${expireds.length} job' statuses has been cleared").when(expireds.nonEmpty)
    } yield ()

  override def queued: ZIO[Clock with Logger, Throwable, Int] =
    Q.size.commit
}

object JobQueue {
  def init(capacity: Int, TTL: Long): ZIO[Clock with Logger, Throwable, JobQueue] =
    for {
      q <- TQueue.make[Job](capacity).commit
      s <- TRef.make(TreeMap.empty[String, JobStatus]).commit
    } yield new JobQueue(q, s, TTL)
}
