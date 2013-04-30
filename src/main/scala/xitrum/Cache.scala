package xitrum

import java.util.concurrent.TimeUnit
import scala.util.control.NonFatal

object Cache extends Logger {
  val cache = new cleakka.Cache(10)  // FIXME

  def remove(key: Any) {
    cache.remove(key)
  }

  def removeAction(actionClass: Class[_ <: Action]) {
    val keyPrefix = pageActionPrefix(actionClass)
    cache.remove(keyPrefix)
  }

  def pageActionPrefix(actionClass: Class[_ <: Action]): String =
    "xitrum/page-action/" + actionClass.getName

  //---------------------------------------------------------------------------

  def put(key: Any, value: AnyRef) {
    if (logger.isDebugEnabled) logger.debug("Cache put: " + key)
    cache.put(key, value)
  }

  def putSecond(key: Any, value: AnyRef, seconds: Int) {
    if (logger.isDebugEnabled) logger.debug("Cache put (" + seconds + "s): " + key)
    cache.put(key, value, seconds)
  }
  def putMinute(key: Any, value: AnyRef, minutes: Int) { putSecond(key, value, minutes * 60) }
  def putHour  (key: Any, value: AnyRef, hours:   Int) { putMinute(key, value, hours   * 60) }
  def putDay   (key: Any, value: AnyRef, days:    Int) { putHour  (key, value, days    * 24) }

  def putIfAbsent(key: Any, value: AnyRef) {
    if (logger.isDebugEnabled) logger.debug("Cache putIfAbsent: " + key)
    cache.putIfAbsent(key, value)
  }

  def putIfAbsentSecond(key: Any, value: AnyRef, seconds: Int) {
    if (logger.isDebugEnabled) logger.debug("Cache putIfAbsent (" + seconds + "s): " + key)
    cache.putIfAbsent(key, value, seconds)
  }
  def putIfAbsentMinute(key: Any, value: AnyRef, minutes: Int) { putIfAbsentSecond(key, value, minutes * 60) }
  def putIfAbsentHour  (key: Any, value: AnyRef, hours:   Int) { putIfAbsentMinute(key, value, hours   * 60) }
  def putIfAbsentDay   (key: Any, value: AnyRef, days:    Int) { putIfAbsentHour  (key, value, days    * 24) }

  //---------------------------------------------------------------------------

  /**
   * Gets data from cache with type cast.
   * Application version up etc. may cause cache restoring to be failed.
   * In this case, we remove the cache.
   */
  def get[T](key: Any): Option[T] = {
    if (!Config.productionMode) return None
    cache.get[T](key)
  }

  def tryCacheSecond[T <: AnyRef](key: Any, secs: Int)(f: => T): T = {
    get[T](key).getOrElse {
      val value = f
      putIfAbsentSecond(key, value, secs)
      value
    }
  }

  def tryCacheMinute[T <: AnyRef](key: String, minutes: Int)(f: => T): T = tryCacheSecond(key, minutes * 60)(f)
  def tryCacheHour  [T <: AnyRef](key: String, hours:   Int)(f: => T): T = tryCacheMinute(key, hours   * 60)(f)
  def tryCacheDay   [T <: AnyRef](key: String, days:    Int)(f: => T): T = tryCacheHour  (key, days    * 24)(f)
}
