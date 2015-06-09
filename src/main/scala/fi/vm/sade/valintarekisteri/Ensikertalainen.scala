package fi.vm.sade.valintarekisteri

import org.http4s.server._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._

object Ensikertalainen {
  val service = HttpService {
    case GET -> Root / "ensikertalaisuus" / oid =>
      Ok(jObjectFields(
        "henkilo" -> jString(oid),
        "ensikertalainen" -> jBool(true)))
  }
}
