package dispatch

import java.net.IDN
import java.lang.IllegalArgumentException

// This will be thrown if the conversion from an IDN domain to the pure-ascii form fails
// Otherwise, the exception that is caused will be thrown.
case object InternationalDomainConversionException extends Exception

object IDNDomainHelpers {

  def safeConvert(domain: String): String = {
    try {
      IDN.toASCII(domain)
    } catch {
      case error: IllegalArgumentException => throw InternationalDomainConversionException
      case error => throw error
    }
  }

}
