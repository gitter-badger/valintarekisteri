package fi.vm.sade.valintarekisteri

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.Server
import scalaz.concurrent.Task
import scala.util.Try

object Main extends App {

  val cliOptions = args match {
    case Array(port, jdbcUrl) if Try(port.toInt).isSuccess => Options(port = port.toInt, jdbcUrl = jdbcUrl)
    case Array(port) if Try(port.toInt).isSuccess =>  Options(port = port.toInt)
    case default => Options()

  }

  ValintarekisteriServer.server(cliOptions).run.server.awaitShutdown()



}


object ValintarekisteriServer {

  def server(options: Options, priorAcceptances: List[VastaanottoTieto] = Nil):Task[ServerData] = {
    BlazeBuilder.bindHttp(options.port)
      .mountService(Ensikertalainen.service, "/")
      .mountService(Vastaanotto(options.jdbcUrl).service)
      .start.map(ServerData(options.port, _))
  }




}

case class Options(port: Int = 8080, jdbcUrl: String = "jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1")


case class ServerData(port:Int, server: Server)
