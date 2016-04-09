package dispatch.as.stream

import dispatch._

object Lines {
  def apply[U](f: String => U) =
    new stream.StringsByLine[Unit] {
      def onStringBy(string: String) {
        f(string)
      }
      def onCompleted = ()
    }
}
