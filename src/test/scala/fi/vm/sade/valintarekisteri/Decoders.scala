package fi.vm.sade.valintarekisteri

import argonaut.CodecJson
import argonaut.Argonaut._
import org.http4s.argonaut.ArgonautInstances



trait Decoders extends ArgonautInstances {

  implicit val kohdeCodec:CodecJson[VastaanotonKohde] = CodecJson(
    vt => jString(vt.id),
    c => for (id <- c.as[String]) yield VastaanotonKohde(id)

  )

  implicit val vastaanottoTietoDecoder = jdecode3L(VastaanottoTieto.apply)("henkilo", "hakukohde", "timestamp")

  implicit val vastaanottoDecoder = jsonOf[Seq[VastaanottoTieto]]


}
