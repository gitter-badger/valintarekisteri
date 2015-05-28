package fi.vm.sade

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.Server
import scalaz.concurrent.Task
import scala.util.Try
import fi.vm.sade.valintarekisteri.Vastaanotto

object ValintarekisteriServer extends App {

  override def main(args:Array[String]) = {
    server(Options(args)).run.server.awaitShutdown()
  }

  def server(options: Options):Task[ServerData] = {
    BlazeBuilder.bindHttp(options.port)
      .mountService(Ensikertalainen.service, "/")
      .mountService(Vastaanotto.service)
      .start.map(ServerData(options.port, _))
  }




}

case class Options(port: Int)

object Options{
  def apply(args: Array[String]): Options = args match {
    case Array(port) if Try(port.toInt).isSuccess =>  Options(port.toInt)
    case default => Options(8080)

  }
}

case class ServerData(port:Int, server: Server)
