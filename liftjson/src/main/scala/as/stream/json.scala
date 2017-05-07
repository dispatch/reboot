package dispatch.as.lift.stream

import dispatch.stream.StringsByLine
import net.liftweb.json.{ JsonParser, JValue }

object Json {
  def apply[T](f: JValue => T) =
    new StringsByLine[Unit] {
      def onStringBy(string: String) = {
        f(JsonParser.parse(string))
        ()
      }
      def onCompleted = ()
    }
}
