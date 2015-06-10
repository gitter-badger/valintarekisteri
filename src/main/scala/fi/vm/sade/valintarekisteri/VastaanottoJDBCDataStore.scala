package fi.vm.sade.valintarekisteri

import slick.dbio.Effect.Read
import slick.profile.{FixedSqlAction, FixedSqlStreamingAction}

import scalaz.concurrent.Task
import scalaz.stream._

import slick.driver.H2Driver.api._
import scala.concurrent.Future
import scalaz.{-\/, \/-}
import slick.jdbc.meta.MTable
import scala.reflect.ClassTag
import slick.backend.DatabasePublisher
import org.reactivestreams.{Subscription, Subscriber}
import scalaz.stream.async.mutable.{Topic, Signal}
import scalaz.-\/
import scala.Some
import scalaz.\/-

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



  override val henkiloQuery: Channel[Task, String, Process[Task, VastaanottoTieto]] =  dbStreamChannel[(String, VastaanotonKohde, Long)].contramap[String](
    (henkilo: String) => vastaanottos.filter(_.henkilo === henkilo).result
  ).mapOut(
    _.map{
      case (henkilo, kohde, timestamp) => VastaanottoTieto(henkilo, kohde, timestamp)
    }
   )


  def dbStreamTask[R: ClassTag](action: slick.dbio.DBIOAction[Seq[R], Streaming[R], Nothing]):Task[Process[Task,R]]  = {


    val queue = async.boundedQueue[R](1)



    trait RequestableSubscriber[T] extends Subscriber[T] {
      var sub: Subscription

      var batchSize: Long

      def requestBatch = sub.request(batchSize)


    }

    val endSignal = async.signalOf(false)


    val subscriber:RequestableSubscriber[R] = new RequestableSubscriber[R] {

      var sub:Subscription = _

      var batchSize = 1L


      override def onNext(t: R): Unit = {

        val orderAgain:Task[Unit] = Task.delay[Unit]{
          sub.request(1)

        }
        queue.enqueueOne(t).onFinish((result) => orderAgain).run

      }

      override def onComplete(): Unit = {
        sub.cancel()
        endSignal.set(true).run

      }

      override def onSubscribe(s: Subscription): Unit = {
        sub = s
        sub.request(1)

      }

      override def onError(t: Throwable): Unit = queue.fail(t)
    }




    Task{
      val publish: DatabasePublisher[R] = db.stream(action)
      publish.subscribe(subscriber)
      endSignal.discrete.takeWhile(!_).run.onFinish((result) => queue.close).runAsync((result) => {})
      queue.dequeue
    }

  }

  def dbStreamChannel[R : ClassTag]: Channel[Task, slick.dbio.DBIOAction[Seq[R], Streaming[R], Nothing], Process[Task,R]] = channel.lift(dbStreamTask)

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




