package acceptance

import _root_.argonaut.Json
import _root_.argonaut.Json._
import fi.vm.sade.{Options, ValintarekisteriServer}
import java.net.ServerSocket
import java.io.IOException
import org.http4s._
import org.http4s.client.Client
import scalaz.stream.Process
import org.http4s.Uri.Authority
import fi.vm.sade.ServerData
import scala.Some
import scodec.bits.ByteVector

class EnsimmainenVastaanotto extends AcceptanceSpec {

  feature("The user can accept a place of study") {
    info("As an applicant")
    info("I want to be able to accept a place of study")
    info("So that I can start studying")

    scenario("Accepting with no prior acceptance") {
      Given ("An applicant has not accepted a place of study earlier")
      implicit val server: ServerData = ValintarekisteriServer.server(Options(freePort)).run
      implicit val client: Client = org.http4s.client.blaze.defaultClient


      When ("The Applicant accepts a place of study")

      val vastaanotto: Json = jObjectFields("henkilo" -> jString("henkilo-oid"),
        "hakukohde" -> jString("hakukohde-oid"))

      val acceptanceSave= postJson("/vastaanotto")(vastaanotto)



      Then ("He gets a succesfull result")
      acceptanceSave.run.status should be (Status.Ok)

      server.server.shutdown.run

    }


  }



  def freePort:Int =  {
    var maybesocket:Option[ServerSocket] = None
    (try {
      maybesocket = Some(new ServerSocket(0))
      for (socket <- maybesocket) yield {
        socket.setReuseAddress(true)
        socket.getLocalPort()
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



  def request(method: Method, relationalUri: String, body: EntityBody = EmptyBody, headers: Headers = Headers.empty)(implicit server:ServerData, client: Client) = {
    val testServer = Uri(authority = Some(Authority(port = Some(server.port))))
    val req =   Request(method = method, uri = resolve(testServer, Uri(path = relationalUri)), body = body, headers = headers)
    client(req)

  }

  def get(relationalUri: String)(implicit server:ServerData, client: Client) = {
    request(Method.GET, relationalUri)
  }

  def post(relationalUri:String)(body: EntityBody = EmptyBody)(implicit server:ServerData, client: Client) = {
    request(Method.POST, relationalUri, body)
  }

  def postJson(relationalUri:String)(json: Json)(implicit server:ServerData, client: Client) = {

    val body = Process(ByteVector(json.toString.getBytes)).toSource

    request(Method.POST, relationalUri, body, Headers(Header("Content-Type", "application/json")))
  }

}
