package fi.vm.sade.valintarekisteri


import argonaut._, Argonaut._

import scalaz.stream._
import scodec.bits.ByteVector
import scalaz.concurrent.Task
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scalaz.stream.Sink
import scalaz.stream.Process._
import org.http4s._
import org.http4s.argonaut.ArgonautInstances


class VastaanottoSpec extends UnitSpec with ArgonautInstances with ResponseSupport {

  behavior of "Vastaanotto"

  val vastaanottoJson = jObjectFields("henkilo" -> jString("henkilo-oid"),
    "hakukohde" -> jString("hakukohde-oid"))

  val body = Process(ByteVector(vastaanottoJson.toString().getBytes)).toSource

  val saveRequest: Request = Request(uri = Uri(path = "vastaanotto"), method = Method.POST, body = body)

  val readRequest: Request = Request(uri = Uri(path = "vastaanotto", query = Query.fromPairs("henkilo" -> "henkilo-oid")), method = Method.GET)


  it should "return ok for a valid request" in {


    val response = Vastaanotto(TestDb.freeJdbcUrl).service(saveRequest).run

    response.exists(_.status == Status.Ok) should be (true)


  }


  it should "save the given result" in {

    val storedResults:mutable.Buffer[VastaanottoTieto] =  ListBuffer()
    val dataStore =  new DataStore {
      override val henkiloQuery: Channel[Task, String, Seq[VastaanottoTieto]] = Process().toSource
      override val newItems: Sink[Task, VastaanottoTieto] =       sink.lift((vt:VastaanottoTieto) => Task{saveToBuffer(storedResults)(vt)})

    }

    new Vastaanotto(dataStore).service(saveRequest).run


    storedResults.exists((vt) => vt.henkiloOid == "henkilo-oid" && vt.hakukohdeOid == HakukohdeOid("hakukohde-oid"))  should be (true)
  }

  it should "query the datastore with henkiloOid" in {

    val tieto1 = VastaanottoTieto("henkilo-oid", "hakukohde-oid")
    val tieto2 = VastaanottoTieto("henkilo-oid", "hakukohde-oid2")

    val dataStore =  new DataStore {

      override val henkiloQuery: Channel[Task, String, Seq[VastaanottoTieto]] = channel.lift{
        (henkiloOid:String) =>
          henkiloOid match {
            case "henkilo-oid" =>
              Task{Seq(tieto1,tieto2)}
            case default =>
              Task{Seq()}
          }
      }
      override val newItems: Sink[Task, VastaanottoTieto] =       sink.lift((vt:VastaanottoTieto) => Task[Unit]{})

    }

    val resp = new Vastaanotto(dataStore).service(readRequest).flatMap((resp) => vastaanottoDecoder.decode(resp.get).validation).run

    resp.getOrElse(Seq()) should( contain (tieto1) and contain (tieto2))


  }


  def saveToBuffer(buffer: mutable.Buffer[VastaanottoTieto])(vt:VastaanottoTieto) {
    buffer += vt
  }


}


