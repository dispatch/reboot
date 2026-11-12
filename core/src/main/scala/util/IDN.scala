package dispatch

import java.net.IDN
import java.lang.IllegalArgumentException

// This will be thrown if the conversion from an IDN domain to the pure-ascii form
// fails. Otherwise, the exception that is caused will be thrown.
case class InternationalDomainConversionException(
  message: String,
  cause: IllegalArgumentException
) extends Exception(message, cause)

object IDNDomainHelpers {

  /**
   * Convert a domain name to its pure-ASCII (punycode) form, rejecting any
   * input that is not exactly a host: URL metacharacters such as `/ ? # @ :`
   * must not be able to change which authority a request built from the result
   * targets.
   *
   * `USE_STD3_ASCII_RULES` makes `IDN.toASCII` reject those characters. As a
   * belt-and-braces check, the converted host is round-tripped through the
   * multi-argument `URI` constructor, whose host component cannot contain
   * authority metacharacters, and the parsed host must equal the input.
   *
   * Bracketed IPv6 literals (e.g. `[::1]`) are exempt from STD3 because that
   * flag only permits LDH characters; the URI round-trip still applies to them.
   */
  def safeConvert(domain: String): String = {
    val bracketedIPv6 = domain.startsWith("[") && domain.endsWith("]")
    val ascii =
      try {
        if (bracketedIPv6) IDN.toASCII(domain)
        else IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES)
      } catch {
        case error: IllegalArgumentException =>
          throw InternationalDomainConversionException(
            "Error converting domain name to ASCII",
            error
          )
        case error: Throwable => throw error
      }
    // The multi-argument constructor quotes illegal characters instead of
    // rejecting them, so re-parse and require the host to survive unchanged.
    // getHost is null when the authority was not parsed as a server-based
    // authority (e.g. underscores), which also fails the check below.
    val parsedHost =
      try {
        new java.net.URI("http", null, ascii.toLowerCase(java.util.Locale.ROOT), -1, "/", null, null).getHost
      } catch {
        case error: java.net.URISyntaxException => null
      }
    if (parsedHost == null || !parsedHost.equalsIgnoreCase(ascii)) {
      throw InternationalDomainConversionException(
        s"Not a valid host name: '$domain'",
        new IllegalArgumentException("host string would change the request authority")
      )
    }
    ascii
  }

}
