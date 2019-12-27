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
}
