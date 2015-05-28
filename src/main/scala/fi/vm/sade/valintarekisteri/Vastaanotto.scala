package fi.vm.sade.valintarekisteri

import org.http4s.server.HttpService
import org.http4s.dsl._
import org.http4s.argonaut.ArgonautInstances
import argonaut._, Argonaut._
import scala.compat.Platform


object Vastaanotto extends ArgonautInstances {

  implicit val vastaanottoTietoDecoder = jdecode2L(VastaanottoTieto.apply)("henkilo", "hakukohde")

  implicit val decoder = jsonOf[VastaanottoTieto]

  val service = HttpService {
    case req@ POST -> Root / "vastaanotto" =>
      req.decode[VastaanottoTieto]{
        vt =>
          Ok()
      }

  }


}


case class VastaanottoTieto(henkiloOid: String, hakukohdeOid: String, timestamp: Long)


object VastaanottoTieto {

  def apply(henkiloOid: String, hakukohdeOid: String): VastaanottoTieto = VastaanottoTieto(henkiloOid, hakukohdeOid, Platform.currentTime)


}