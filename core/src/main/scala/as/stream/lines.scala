package dispatch.as.stream

import dispatch._

import com.ning.http.client._

object Lines {
  def apply[U](f: String => U) =
    new stream.StringsByLine[Unit] {
      def onStringBy(string: String) = {
        f(string)
        ()
      }
      def onCompleted = ()
    }
}
