package fi.vm.sade.valintarekisteri

import org.http4s.server.HttpService



import org.http4s.dsl._
import org.http4s.argonaut.ArgonautInstances
import _root_.argonaut._, Argonaut._

import scala.compat.Platform
import scalaz.stream._
import scalaz.concurrent.Task
import org.http4s._
import org.http4s.headers.`Transfer-Encoding`
import org.http4s.EntityEncoder.Entity
import org.http4s.dsl./
import scodec.bits.ByteVector
import java.nio.charset.StandardCharsets


class Vastaanotto(val dataStore:DataStore[VastaanottoTieto, String]) extends ArgonautInstances {

  implicit val kohdeCodec:CodecJson[VastaanotonKohde] = CodecJson(
    vt => jString(vt.id),
    c => for (id <- c.as[String]) yield VastaanotonKohde(id)

  )


  implicit val vastaanottoTietoDecoder: DecodeJson[VastaanottoTieto] =     DecodeJson(c => for {
    henkiloOid <- (c --\ "henkilo").as[String]
    kohde <- (c --\ "hakukohde").as[VastaanotonKohde]
    timestamp <- (c --\ "timestamp").as[Long].option
  } yield VastaanottoTieto(henkiloOid, kohde, timestamp.getOrElse(Platform.currentTime)))


  implicit val vastaanottoTietoEncoder = jencode3L((vt: VastaanottoTieto) => (vt.henkiloOid, vt.hakukohdeOid, vt.timestamp))("henkilo", "hakukohde", "timestamp")

  implicit val decoder = jsonOf[VastaanottoTieto]


  implicit val encoder: EntityEncoder[VastaanottoTieto] = jsonEncoderOf[VastaanottoTieto]


  object HenkiloOid extends QueryParamDecoderMatcher[String]("henkilo")


  def jsonSeqEncoder[A](implicit W: EntityEncoder[A]): EntityEncoder[Process[Task, A]] = new EntityEncoder[Process[Task, A]] {

    val start = Process.emit(ByteVector("[".getBytes(StandardCharsets.UTF_8))).toSource
    val end = Process.emit(ByteVector("]".getBytes(StandardCharsets.UTF_8)))

    val comma = Process.constant(ByteVector(",".getBytes(StandardCharsets.UTF_8)))



    override def toEntity(a: Process[Task, A]): Task[Entity] = {
      val entityBody: Process[Task, ByteVector] = start ++  a.flatMap((a: A) => {
         Process.await(W.toEntity(a))(_.body)
      }).interleave(comma).dropLast ++ end
      Task.now(Entity(entityBody, None))
    }

    override def headers: Headers = W.headers
  }

  implicit val vastaanottoProcessEncoder = jsonSeqEncoder[VastaanottoTieto]

  def service = HttpService {
    case req@ POST -> Root / "vastaanotto" =>


      req.decode[VastaanottoTieto]{
        vt =>
          save(vt).run.flatMap((vt) => Ok())
      }

    case req@ GET -> Root / "vastaanotto" :? HenkiloOid(henkilo) =>

      val query = (Process(Seq(henkilo)).flatMap(Process.emitAll).toSource through dataStore.henkiloQuery).flatMap(
        identity
      )

      Ok(query).withHeaders(`Transfer-Encoding`(TransferCoding.chunked))




  }


  def save(vt: VastaanottoTieto): Process[Task, VastaanottoTieto] = Process(vt).toSource observe dataStore.newItems
}

import slick.driver.H2Driver.api._

object Vastaanotto {

  def apply(jdbcUrl:String):Vastaanotto = new Vastaanotto(new VastaanottoJDBCDataStore(Database.forURL(jdbcUrl)))

}



sealed trait VastaanotonKohde {

  val id: String

}

object VastaanotonKohde {

  def apply(id:String): VastaanotonKohde = HakukohdeOid(id)

  def unapply(vt:VastaanotonKohde): Option[String] = vt match {
    case ho: HakukohdeOid => HakukohdeOid.unapply(ho)
  }

}

case class HakukohdeOid(oid: String) extends VastaanotonKohde{
  override val id: String = oid
}

case class VastaanottoTieto(henkiloOid: String, hakukohdeOid: VastaanotonKohde, timestamp: Long)


object VastaanottoTieto {

  def apply(henkiloOid: String, kohde: String): VastaanottoTieto = kohde match {
    case hakukohdeOid => VastaanottoTieto(henkiloOid, HakukohdeOid(hakukohdeOid), Platform.currentTime)


  }


}

case class PriorAcceptance(prior: VastaanottoTieto) extends Exception


