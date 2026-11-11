package dispatch.spec

import org.scalacheck._
import org.scalacheck.Prop._

object UriSpecification extends Properties("Uri") {
  /** java.net.URLDecoder should *NOT* be used for testing URI segment decoding
   *  because it implements completely different functionality: query parameter decoding
   */
  property("Encodes and decodes basic strings") = Prop.forAll { (path: String) =>
    !path.contains(":") ==> {
      new java.net.URI(dispatch.UriEncode.path(path)).getPath == path
    } // else Prop.throws(classOf[java.net.URISyntaxException])
  }

  /** if there is nothing to escape, encoder must return original reference */
  property("Does nothing if there's nothing eo encode") = Prop.forAll(Gen.choose(0,100)) { (n: Int) =>
    val path = "A" * n
    dispatch.UriEncode.path(path) eq path
  }

  property("Encodes emoji correctly") = forAll(Gen.const("unused")) { (sample: String) =>
    val path = "roma🇮🇹"
    new java.net.URI(dispatch.UriEncode.path(path)).getPath == (path)
  }

  /** A dot-segment would let a single appended segment escape its parent path:
   *  async-http-client resolves it when the URL is set, so `/ "users" / ".."`
   *  yields `/`. Every segment-appending verb must refuse one.
   */
  property("Rejects dot-segments appended as a path segment") = forAllNoShrink(Gen.oneOf(".", "..")) { (segment: String) =>
    val users = dispatch.host("api.example.com") / "users"
    Prop.throws(classOf[IllegalArgumentException])(users / segment) &&
    Prop.throws(classOf[IllegalArgumentException])(users.appendSegment(segment)) &&
    Prop.throws(classOf[IllegalArgumentException])(users /? Some(segment)) &&
    Prop.throws(classOf[IllegalArgumentException])(users.appendOptionalSegment(Some(segment)))
  }

  property("Accepts segments that contain dots but are not dot-segments") =
    forAll(Gen.oneOf("...", ".well-known", "..a", "a..", "file.txt", "1.0")) { (segment: String) =>
      (dispatch.host("api.example.com") / "users" / segment).url ?=
        s"http://api.example.com/users/$segment"
    }
}
