package dispatch

import scala.concurrent.duration.Duration
import org.jboss.netty.util.{Timer, HashedWheelTimer}

object Defaults {
  implicit def executor = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val timer: Timer = InternalDefaults.timer
}
