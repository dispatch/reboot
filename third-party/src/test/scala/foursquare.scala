package dispatch.clients

import dispatch._
import org.specs._

object FoursquareSpec extends Specification {
  "A basic foursquare request that requires auth" should {
    "be able to be formed normally" in new context {
      val builtReq = Auth(("key", "secret"))(req).to_uri.toString

      builtReq.contains("api.foursquare.com/v2/venues/search") must_== true
      builtReq.contains("client_id=key") must_== true
      builtReq.contains("client_secret=secret") must_== true
      builtReq.contains("ll=123.123%2C456.456") must_== true
      builtReq.contains("v=19700101") must_== true
      builtReq.contains("https://") must_== true
    }

    "be able to be formed normally through other apply" in new context {
      val builtReq = Auth("key", "secret")(req).to_uri.toString

      builtReq.contains("api.foursquare.com/v2/venues/search") must_== true
      builtReq.contains("client_id=key") must_== true
      builtReq.contains("client_secret=secret") must_== true
      builtReq.contains("ll=123.123%2C456.456") must_== true
      builtReq.contains("v=19700101") must_== true
      builtReq.contains("https://") must_== true
    }
  }
}

trait context {
  val req = /\ / "v2" / "venues" / "search" <<? Map("ll" -> "123.123,456.456", "v" -> "19700101")
}
