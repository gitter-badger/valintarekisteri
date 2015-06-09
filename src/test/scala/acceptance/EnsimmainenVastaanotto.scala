package acceptance

import _root_.argonaut.Json
import _root_.argonaut.Json._
import java.net.ServerSocket
import java.io.IOException
import org.http4s._
import org.http4s.client.Client
import scalaz.stream.Process
import scodec.bits.ByteVector
import fi.vm.sade.valintarekisteri._
import org.http4s.Uri.Authority
import scala.Some

class EnsimmainenVastaanotto extends AcceptanceSpec with ResponseSupport {

  feature("The user can accept a place of study") {
    info("As an applicant")
    info("I want to be able to accept a place of study")
    info("So that I can start studying")

    scenario("Accepting with no prior acceptance") {
      Given ("An applicant has not accepted a place of study earlier")
      implicit val server: ServerData = ValintarekisteriServer.server(Options(freePort, TestDb.freeJdbcUrl)).run
      implicit val client: Client = org.http4s.client.blaze.defaultClient


      When ("The Applicant accepts a place of study")

      val vastaanotto: Json = jObjectFields("henkilo" -> jString("henkilo-oid"),
        "hakukohde" -> jString("hakukohde-oid"))

      val acceptanceSave= postJson("/vastaanotto")(vastaanotto)



      Then ("He gets a succesfull result")
      val response = acceptanceSave.run

      //println((response.body |> decodeUtf8).runLog.run)

      response.status should be (Status.Ok)

      server.server.shutdown.run

    }

    scenario("checking vastaanotto") {

      Given ("An applicant has accepted a place of study earlier in the given application round")
      val acceptances = List(VastaanottoTieto("henkilo-oid", "hakukohde-oid2"))
      val dburl = TestDb.freeJdbcUrl
      implicit val server: ServerData = ValintarekisteriServer.server(Options(freePort, dburl), acceptances).run
      implicit val client: Client = org.http4s.client.blaze.defaultClient
      val aiempivastaanotto: Json = jObjectFields("henkilo" -> jString("henkilo-oid"),
        "hakukohde" -> jString("hakukohde-oid"))

      postJson("/vastaanotto")(aiempivastaanotto).run

      import scala.concurrent.duration._
      import scala.concurrent.Await
      import slick.driver.H2Driver.api._

      val vastaanottos = TableQuery[Vastaanottos]

      println(Await.result(Database.forURL(dburl).run(vastaanottos.result), Duration.Inf))

      When ("The Applicant checks for acceptances for a place of study")

      val getRequest = get("/vastaanotto", "henkilo" -> "henkilo-oid")

      Then ("He gets a result containg data on the previously accepted place of study")

      val vastaanotot = getRequest.flatMap((resp) => vastaanottoDecoder.decode(resp).validation).run.getOrElse(Seq())

      vastaanotot.map((vt) => vt.henkiloOid -> vt.hakukohdeOid.id) should be (Seq("henkilo-oid" -> "hakukohde-oid"))


    }
  }





  def freePort:Int =  {
    var maybesocket:Option[ServerSocket] = None
    (try {
      maybesocket = Some(new ServerSocket(0))
      for (socket <- maybesocket) yield {
        socket.setReuseAddress(true)
        socket.getLocalPort
      }
    } finally {
      if (maybesocket.isDefined) {
        try {
          maybesocket.foreach(_.close())
        } catch { case e:IOException =>
        }
      }
    }).get

  }


  import org.http4s.Http4s._



  def request(method: Method, relationalUri: String, body: EntityBody = EmptyBody, headers: Headers = Headers.empty, params: Seq[(String, String)] = Seq())(implicit server:ServerData, client: Client) = {
    val testServer = Uri(authority = Some(Authority(port = Some(server.port))))
    val req =   Request(method = method, uri = resolve(testServer, Uri(path = relationalUri, query = Query.fromPairs(params:_*))), body = body, headers = headers)
    client(req)

  }

  def get(relationalUri: String, params: (String, String)*)(implicit server:ServerData, client: Client) = {
    request(Method.GET, relationalUri, params = params)
  }

  def post(relationalUri:String)(body: EntityBody = EmptyBody)(implicit server:ServerData, client: Client) = {
    request(Method.POST, relationalUri, body)
  }

  def postJson(relationalUri:String)(json: Json)(implicit server:ServerData, client: Client) = {

    val body = Process(ByteVector(json.toString().getBytes)).toSource

    request(Method.POST, relationalUri, body, Headers(Header("Content-Type", "application/json")))
  }

}


