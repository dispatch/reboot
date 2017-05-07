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

  def safeConvert(domain: String): String = {
    try {
      IDN.toASCII(domain)
    } catch {
      case error: IllegalArgumentException =>
        throw InternationalDomainConversionException(
          "Error converting domain name to ASCII",
          error
        )
      case error: Throwable => throw error
    }
  }

}
