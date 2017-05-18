package dispatch.spec

import org.scalacheck._
import net.liftweb.json._
import JsonDSL._
import dispatch._
import org.asynchttpclient._
import org.mockito.Mockito._

object BasicSpecification extends Properties("Lift Json") {
  import Prop.forAll

  property("parse json") = forAll(Gen.alphaStr) { sample =>
    val mockedResponse = mock(classOf[Response])
    when(mockedResponse.getResponseBody).thenReturn(compactRender(
      ("out" -> sample)
    ))

    val result = as.lift.Json(mockedResponse)

    result == JObject(JField("out", JString(sample)) :: Nil)
  }
}
