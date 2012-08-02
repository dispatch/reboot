package dispatch.clients

import dispatch._
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

object FoursquareSpec extends Specification {

  "A basic foursquare request that requires auth" should {
    "be able to be formed normally" in new foursquareContext {
      val request = fsq.buildUri("/venues/search", Map("q" -> "drinks"))
      val builtReq = request.build().getUrl()
      println(builtReq)

      builtReq.contains("https://") must beTrue
      builtReq.contains("api.foursquare.com/v2") must beTrue
      builtReq.contains("/venues/search") must beTrue
      builtReq.contains("client_id=aa") must beTrue
      builtReq.contains("client_secret=bb") must beTrue
      builtReq.contains("q=drinks") must beTrue
    }

    "executing a request should give us back a standard http code" in new foursquareContext {
      val request = for {
        req <- fsq.call("/venues/search", Map("q" -> "drinks"))
      } yield (req)

      request onComplete {
        case ret =>
          isInstanceOf[Some[_]] must beTrue.eventually
      }
    }
  }

}

trait foursquareContext extends Scope {
  val creds = OAuth2Creds("aa", "bb")
  val fsq = Foursquare(creds)
  fsq.setVersion("v2")
}

