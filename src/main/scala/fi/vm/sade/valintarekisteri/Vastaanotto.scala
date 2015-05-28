package fi.vm.sade.valintarekisteri

import org.http4s.server.HttpService
import org.http4s.dsl._
import org.http4s.argonaut.ArgonautInstances
import argonaut.DecodeJson
import scala.compat.Platform
import scalaz.stream.Process
import scodec.bits.ByteVector
import scalaz.stream.Process.Halt
import scalaz.stream.Cause.Error


object Vastaanotto extends ArgonautInstances {

  implicit val vastaanottoTietoDecoder = DecodeJson(c => for {
    name <- (c --\ "henkilo").as[String]
    age <- (c --\ "hakukohde").as[String]
  } yield VastaanottoTieto(name, age))

  implicit val decoder = jsonOf[VastaanottoTieto]

  val service = HttpService {
    case req@ POST -> Root / "vastaanotto" =>
      req.decode[VastaanottoTieto]{
        vt =>
          Ok()
      }

  }


}


case class VastaanottoTieto(henkiloOid: String, hakukohdeOid: String, timestamp: Long = Platform.currentTime)
