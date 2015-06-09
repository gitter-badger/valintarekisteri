package fi.vm.sade.valintarekisteri

import slick.driver.H2Driver.api._
import scalaz.stream.Process
import scala.concurrent.Await
import scala.concurrent.duration.Duration


class VastaanottoDatastoreSpec extends UnitSpec {

  behavior of "VastaanottoDataStore"

  val vastaanottos = TableQuery[Vastaanottos]



  it should "save the new Vastaanottotieto" in {




    val db: Database = Database.forURL(TestDb.freeJdbcUrl)


    val dataStore = new VastaanottoJDBCDataStore(db)

    val tieto = VastaanottoTieto("henkilo", "koulu")
    (Process(tieto).toSource to dataStore.newItems).run.run


    import scala.concurrent.duration._
    import scala.concurrent.Await
    Await.result(db.run(vastaanottos.result), Duration.Inf) should be (Vector((tieto.henkiloOid,tieto.hakukohdeOid,tieto.timestamp)))



  }


  it should "show queried Vastaanottos" in {



    val vastaanotto1 = VastaanottoTieto("henkilo1", "koulu1")
    val vastaanotto2 = VastaanottoTieto("henkilo2", "koulu2")

    val db: Database = Database.forURL(TestDb.freeJdbcUrl)

    val dataStore = new VastaanottoJDBCDataStore(db)


    Await.result(
      db.run(
        DBIO.seq(
          vastaanottos += (vastaanotto1.henkiloOid, vastaanotto1.hakukohdeOid, vastaanotto1.timestamp),
          vastaanottos += (vastaanotto2.henkiloOid, vastaanotto2.hakukohdeOid, vastaanotto2.timestamp))), Duration.Inf)


    (Process("henkilo1").toSource through dataStore.henkiloQuery).runLast.run.get should be (Seq(vastaanotto1))





  }





}
