package xitrum.imperatively

import xitrum.session.Session

object SessionHolder extends java.lang.ThreadLocal[Session] {
  def session: Session = get
}
