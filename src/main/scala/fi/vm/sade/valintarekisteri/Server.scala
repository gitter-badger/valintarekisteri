package fi.vm.sade

import org.http4s.server.blaze.BlazeBuilder

object BlazeExample extends App {
  BlazeBuilder.bindHttp(8080)
    .mountService(Ensikertalainen.service, "/")
    .run
    .awaitShutdown()
}
