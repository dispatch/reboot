package oauth

import org.asynchttpclient.oauth.{ConsumerKey, RequestToken}
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

/**
  * Tests for oauth / exchange.
  *
  * @author Erik-Berndt Scheper
  * @since 25-01-2017
  *
  */
object ExchangeSpec extends Properties("String") {

  private val safeChars = "[A-Za-z0-9%._~()'!*:@,;-]*"
  private val urlPattern = s"(.*)[?]oauth_token=($safeChars)[&]oauth_signature=($safeChars)".r

  property("signedAuthorize") = forAll { (keyValue: String, tokenValue: String) =>
    import dispatch._
    import dispatch.oauth._

    trait DropboxHttp extends SomeHttp {
      def http: HttpExecutor = Http
    }

    trait DropboxConsumer extends SomeConsumer {
      def consumer: ConsumerKey = new ConsumerKey(keyValue, tokenValue)
    }

    trait DropboxCallback extends SomeCallback {
      def callback: String = "oob"
    }

    trait DropboxEndpoints extends SomeEndpoints {
      def requestToken: String = "https://api.dropbox.com/1/oauth/request_token"

      def accessToken: String = "https://www.dropbox.com/1/oauth/authorize"

      def authorize: String = "https://api.dropbox.com/1/oauth/access_token"
    }

    object DropboxExchange extends Exchange
      with DropboxHttp with DropboxConsumer with DropboxCallback with DropboxEndpoints

    val token = new RequestToken(keyValue, tokenValue)
    val url = DropboxExchange.signedAuthorize(token)

    val urlMatcher = url match {
      case urlPattern(path: String, authToken: String, signature: String) =>
        (path, authToken, signature)
      case _ =>
        ("", "", "") // no match
    }

    val urlPath = urlMatcher._1
    val authToken = urlMatcher._2
    val authSignature = urlMatcher._3

    urlPath.equals(DropboxExchange.authorize) &&
      authToken.length >= keyValue.length &&
      authToken.matches(safeChars) &&
      authSignature.length > 0 &&
      authSignature.matches(safeChars)
  }

}
