package dispatch.as.json4s.stream

import dispatch.stream.StringsByLine
import org.json4s._
import org.json4s.native.JsonMethods._

object Json {
  def apply[T](f: JValue => T) =
    new StringsByLine[Unit] {
      def onStringBy(string: String) = {
        f(parse(string, true))
        ()
      }
      def onCompleted = ()
    }
}
