package fi.vm.sade.valintarekisteri

import scalaz.concurrent.Task
import scalaz.stream._

import slick.driver.H2Driver.api._
import scala.concurrent.Future
import scalaz.{-\/, \/-}
import slick.jdbc.meta.MTable
import scala.reflect.ClassTag

class VastaanottoJDBCDataStore(val db: Database) extends DataStore[VastaanottoTieto, String] {

  val vastaanottos = TableQuery[Vastaanottos]

  import scala.concurrent.ExecutionContext.Implicits.global

  val setup = MTable.getTables(vastaanottos.baseTableRow.tableName).flatMap(
    (tables: Vector[MTable]) => tables.headOption match {
      case None =>
        vastaanottos.schema.create
      case Some(_) =>
        DBIO.successful[Unit]{}
    }
  ).transactionally


  dbTask(setup).attemptRun


  override val henkiloQuery: Channel[Task, String, Process[Task, VastaanottoTieto]] =  dbRunChannel.contramap(
    (henkilo: String) => vastaanottos.filter(_.henkilo === henkilo).result
  ).mapOut((tiedot) =>
    Process.emitAll(tiedot.map{
      case (henkilo, kohde, timestamp) => VastaanottoTieto(henkilo, kohde, timestamp)

    }).toSource)



  def dbRunChannel[R : ClassTag]: Channel[Task, slick.dbio.DBIOAction[R, NoStream, Nothing], R] = channel.lift(dbTask[R])


  def dbTask[R : ClassTag](action: DBIOAction[R, NoStream, Nothing]): Task[R] = {

    Task.async[R] {
      (k) =>
        val runFuture: Future[R] = db.run(action)
        runFuture.onSuccess[Unit] {
          case saved: R =>
            k(\/-(saved))
        }
        runFuture.onFailure[Unit] {
          case error: Throwable => k(-\/(error))
        }
    }
  }

  val saveChannel: Channel[Task, (String, VastaanotonKohde, Long), Int] = dbRunChannel[Int].contramap[(String, VastaanotonKohde, Long)]{

    (tiedot:(String, VastaanotonKohde, Long)) =>
      vastaanottos += tiedot

  }


  override val newItems: Sink[Task, VastaanottoTieto] = saveChannel.contramap[VastaanottoTieto]((vt:VastaanottoTieto) => (vt.henkiloOid, vt.hakukohdeOid, vt.timestamp)).mapOut((i:Int) => {})
}





class Vastaanottos(tag: Tag) extends Table[(String, VastaanotonKohde, Long)](tag, "VASTAANOTTO") {

  implicit val vastaanotonKohdeType = MappedColumnType.base[VastaanotonKohde, String](
    { vk => vk.id },
    VastaanotonKohde.apply
  )

  def henkilo = column[String]("henkilo")
  def kohde = column[VastaanotonKohde]("kohde")
  def timestamp = column[Long]("timestamp")

  def * = (henkilo, kohde, timestamp)
}




