Xitrum is an async and clustered Scala web framework and HTTP(S) server
on top of `Netty <http://netty.io/>`_ and `Akka <http://akka.io/>`_.

::

  +--------------------+
  |      Your app      |
  +--------------------+
  |        Xitrum      |
  | +----------------+ |
  | | Web framework  | |  <-- Akka --> Other instances
  | |----------------| |
  | | HTTP(S) Server | |
  | +----------------+ |
  +--------------------+
  |       Netty        |
  +--------------------+

Please see `Xitrum home page <http://ngocdaothanh.github.io/xitrum>`_.
