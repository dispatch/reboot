package dispatch.spec

import org.scalacheck._

object UriSpecification extends Properties("Uri") {
  /** java.net.URLDecoder should *NOT* be used for testing URI segment decoding
   *  because it implements completely different functionality: query parameter decoding
   */
  property("encode-decode") = Prop.forAll { (path: String) =>
    new java.net.URI(dispatch.UriEncode.path(path)).getPath == path
  }

  /** if there is nothing to escape, encoder must return original reference */
  property("noop") = Prop.forAll(Gen.choose(0,100)) { (n: Int) =>
    val path = "A" * n
    dispatch.UriEncode.path(path) eq path
  }
}
