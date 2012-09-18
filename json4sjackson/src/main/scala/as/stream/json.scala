package dispatch.as.json4s.stream

import dispatch.stream.StringsByLine
import org.json4s.JValue
import org.json4s.jackson.JsonParser

object Json {
  def apply[T](f: JValue => T) =
    new StringsByLine[Unit] {
      def onStringBy(string: String) {
        f(JsonParser.parse(string))
      }
      def onCompleted = ()
    }
}
