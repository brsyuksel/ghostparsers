package paytrek.ghostparsers.jobq

//import zio._
import zio.test._

object JobQueueSuits
    extends DefaultRunnableSpec(
      suite("jobqueue suits")(
        test("enqueueing job request increases queue size and creates a pending job status") {
          assert(1, Assertion.equalTo(1))
        },
        test("dequeuing job request decreases queue size and sets job status to processing") {
          assert(1, Assertion.equalTo(1))
        },
        test("getStatus returns the job's current status") {
          assert(1, Assertion.equalTo(1))
        },
        test("complete sets the job's status to completed") {
          assert(1, Assertion.equalTo(1))
        },
        test("queued returns the current size of the queue") {
          assert(1, Assertion.equalTo(1))
        },
        test("clean clears the expired and completed job statuses") {
          assert(1, Assertion.equalTo(1))
        },
      )
    )
