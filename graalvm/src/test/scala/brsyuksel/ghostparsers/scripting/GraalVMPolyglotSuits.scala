package brsyuksel.ghostparsers.scripting

import zio.test._

object testData {
  val mapScript1 =
    """
      |function map(data) {
      |let d = JSON.parse(data);
      |d["a"] = "aaa";
      |d["x"] = "xxx";
      |return JSON.stringify({data:d, groupBy: null});
      |}
      |""".stripMargin

  val reduceScript1 =
    """
      |function reduce(data) {
      |let d = JSON.parse(data);
      |let res = d.reduce(reducer);
      |return JSON.stringify(res);
      |}
      |
      |const reducer = (acc, e) => {
      |let x = parseInt(acc["val"]);
      |let y = parseInt(e["val"]);
      |let r = x + y;
      |acc["val"] = r.toString();
      |return acc;
      |};
      |""".stripMargin

  val malformedScripts =
    """
      |const map = data => JSON.stringify([]);
      |const reduce = data => JSON.stringify([]);
      |""".stripMargin
}

object GraalVMPolyglotSuits
    extends DefaultRunnableSpec(
      suite("graalvm-polyglot")(
        testM("execute for map returns a returnvalue contains manipulated data") {
          val s = new GraalVMPolyglot(testData.mapScript1) {}
          for {
            r <- s.execute(Map("a" -> "1", "b" -> "2"))
          } yield {
            assert(r.data, Assertion.isNonEmpty) &&
            assert(r.groupBy, Assertion.isNone) &&
            assert(r.data.get("a"), Assertion.isSome(Assertion.equalTo("aaa"))) &&
            assert(r.data.get("b"), Assertion.isSome(Assertion.equalTo("2"))) &&
            assert(r.data.get("x"), Assertion.isSome(Assertion.equalTo("xxx")))
          }
        },
        testM("execute for list of map return only one map data") {
          val s = new GraalVMPolyglot(testData.reduceScript1) {}
          val l = List(Map("val" -> "1", "a" -> "a"), Map("val" -> "2", "a" -> "aa"), Map("val" -> "3", "a" -> "aaa"))
          for {
            r <- s.execute(l)
          } yield {
            assert(r, Assertion.isNonEmpty) &&
            assert(r.get("val"), Assertion.isSome(Assertion.equalTo("6"))) &&
            assert(r.get("a"), Assertion.isSome(Assertion.equalTo("a")))
          }
        },
        testM("execute methods fails with FunctionIsNotFoundError if fn is undefined") {
          val s = new GraalVMPolyglot("") {}
          for {
            mr <- s.execute(Map("a" -> "a")).flip
            rr <- s.execute(List(Map("a" -> "a"))).flip
          } yield {
            assert(mr, Assertion.isSubtype[FunctionIsNotFoundError](Assertion.anything)) &&
            assert(rr, Assertion.isSubtype[FunctionIsNotFoundError](Assertion.anything))
          }
        },
        testM("execute methods fail with MalformedJsonDataError if return value of script fn is not as expected") {
          val s = new GraalVMPolyglot(testData.malformedScripts) {}
          for {
            mr <- s.execute(Map("a" -> "aa")).flip
            rr <- s.execute(List(Map("a" -> "aa"))).flip
          } yield {
            assert(mr, Assertion.isSubtype[MalformedJsonDataError](Assertion.anything)) &&
            assert(rr, Assertion.isSubtype[MalformedJsonDataError](Assertion.anything))
          }
        }
      ))
