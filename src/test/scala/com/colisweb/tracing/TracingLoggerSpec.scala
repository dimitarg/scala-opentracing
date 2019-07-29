package com.colisweb.tracing

import scala.concurrent._
import scala.concurrent.duration._
import org.scalatest._
import cats.implicits._
import cats.data._
import cats.effect._
import java.io.ByteArrayOutputStream
import java.util.UUID
import com.typesafe.scalalogging.StrictLogging
import com.colisweb.tracing.implicits._
import java.io.PrintStream

class TracingLoggerSpec extends FunSpec with StrictLogging with Matchers {

  implicit val slf4jLogger: org.slf4j.Logger = logger.underlying

  describe("JsonTracingLogger") {
    it("Should log trace id as a JSON field when TracingContext has a trace id") {
      val traceId = UUID.randomUUID().toString()
      val context = mockTracingContext(OptionT.none, OptionT.pure(traceId))
      testStdOut(
        context.logger.info("Hello"),
        _ should include(
          s""""dd.trace_id":"$traceId""""
        )
      )
    }

    it("Should log span id as JSON field when TracingContext has a span id") {
      val spanId  = UUID.randomUUID().toString()
      val context = mockTracingContext(OptionT.pure(spanId), OptionT.none)
      testStdOut(
        context.logger.info("Hello"),
        _ should include(
          s""""dd.span_id":"$spanId""""
        )
      )
    }

    it("Should not add anything when TracingContext has neither a span id nor a trace id") {
      val context = mockTracingContext(OptionT.none, OptionT.none)
      testStdOut(
        context.logger.info("Hello"),
        _ should (not include ("trace_id") and not include ("span_id"))
      )
    }
  }

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  private def testStdOut(
      body: IO[Unit],
      assertion: String => Assertion
  ): Assertion = {
    val out    = new ByteArrayOutputStream()
    val stdOut = System.out
    System.setOut(new PrintStream(out))
    (body *> IO.sleep(10.millis)).unsafeRunSync()
    System.setOut(stdOut)
    assertion(out.toString)
  }

  private def mockTracingContext(
      _spanId: OptionT[IO, String],
      _traceId: OptionT[IO, String]
  ) = new TracingContext[IO] {
    def childSpan(operationName: String, tags: Map[String, String]) = ???
    override def spanId: cats.data.OptionT[cats.effect.IO, String]  = _spanId
    override def traceId: cats.data.OptionT[cats.effect.IO, String] = _traceId
    def addTags(tags: Map[String, String]): cats.effect.IO[Unit]    = ???
    def close(): cats.effect.IO[Unit]                               = ???
  }
}
