package brsyuksel.ghostparsers.jobq

import zio._
import zio.duration._
import zio.test._
import io.circe.Json

import brsyuksel.ghostparsers.mock

object JobQueueSuits
    extends DefaultRunnableSpec(
      suite("jobqueue suits")(
        testM("queued returns the current size of the queue") {
          val res = for {
            q <- JobQueue.init(10, 0)
            jobReq = JobRequest(Json.Null)
            _    <- ZIO.sequence(List.fill(5)(q.enqueue(jobReq)))
            size <- q.queued
          } yield assert(size, Assertion.equalTo(5))
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
        testM("enqueueing job request increases queue size and creates a pending job status") {
          val res = for {
            q <- JobQueue.init(10, 0)
            jobReq = JobRequest(Json.Null)
            st   <- q.enqueue(jobReq)
            size <- q.queued
          } yield assert(st.status, Assertion.equalTo(Status.Pending)) && assert(size, Assertion.equalTo(1))
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
        testM("getStatus returns the existing job's current status") {
          val res = for {
            q <- JobQueue.init(10, 0)
            jobReq = JobRequest(Json.Null)
            st <- q.enqueue(jobReq)
            c  <- q.getStatus(st.id)
          } yield {
            assert(st.status, Assertion.equalTo(Status.Pending)) &&
            assert(c.map(_.status), Assertion.isSome(Assertion.equalTo(Status.Pending)))
          }
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
        testM("getStatus returns None for non-existing job") {
          val res = for {
            q <- JobQueue.init(10, 0)
            c <- q.getStatus(java.util.UUID.randomUUID())
          } yield assert(c, Assertion.isNone)
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
        testM("dequeuing job request decreases queue size and sets job status to processing") {
          val res = for {
            q <- JobQueue.init(10, 0)
            jobReq = JobRequest(Json.Null)
            st1 <- q.enqueue(jobReq)
            sz1 <- q.queued
            job <- q.dequeue
            st2 <- q.getStatus(job.id).map(_.map(_.status))
            sz2 <- q.queued
          } yield {
            assert(sz1, Assertion.equalTo(1)) &&
            assert(sz2, Assertion.equalTo(0)) &&
            assert(st1.status, Assertion.equalTo(Status.Pending)) &&
            assert(st2, Assertion.isSome(Assertion.equalTo(Status.Processing)))
          }
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
        testM("complete sets the job's status to completed") {
          val res = for {
            q <- JobQueue.init(10, 0)
            jobReq = JobRequest(Json.Null)
            st1 <- q.enqueue(jobReq)
            st2 <- q.getStatus(st1.id)
            _   <- q.dequeue
            _   <- q.complete(st1.id, Json.Null)
            st3 <- q.getStatus(st1.id)
          } yield {
            assert(st2.map(_.status), Assertion.isSome(Assertion.equalTo(st1.status))) &&
            assert(st3.map(_.status), Assertion.isSome(Assertion.equalTo(Status.Completed)))
          }
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
        testM("complete returns None for non-existing job") {
          val res = for {
            q <- JobQueue.init(10, 0)
            s <- q.complete(java.util.UUID.randomUUID(), Json.Null)
          } yield assert(s, Assertion.isNone)
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
        testM("enqueue blocks job if queue size exceeds capacity") {
          val res = for {
            q <- JobQueue.init(1, 0)
            _ <- q.enqueue(JobRequest(Json.Null))
            uuid = java.util.UUID.randomUUID()
            u <- q
              .enqueue(JobRequest(Json.Null))
              .race(ZIO.sleep(100.milliseconds) >>= { _ =>
                ZIO.succeed(JobStatus(uuid, Status.Completed, 0, 0, Json.Null, None))
              })
            s <- q.queued
          } yield assert(s, Assertion.equalTo(1)) && assert(u.id, Assertion.equalTo(uuid))
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
        testM("clean clears the expired and completed job statuses") {
          val res = for {
            q  <- JobQueue.init(10, 200)
            st <- q.enqueue(JobRequest(Json.Null))
            _  <- q.complete(st.id, Json.Null)
            c1 <- q.getStatus(st.id)
            _  <- ZIO.sleep(300.milliseconds)
            _  <- q.clean
            c2 <- q.getStatus(st.id)
          } yield {
            assert(c1.flatMap(_.completedAt), Assertion.isSome(Assertion.isGreaterThan(0L))) &&
            assert(c2, Assertion.isNone)
          }
          res.provideSome[Any](_ => mock.jobqSuiteEnv)
        },
      )
    )
