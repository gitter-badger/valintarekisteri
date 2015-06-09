package fi.vm.sade.valintarekisteri

import scalaz.stream._
import scalaz.stream.async.mutable.Signal


object TestDb {


  val s:Signal[List[String]] = async.signalOf(List[String]())

  val supply = Process.supply(1).map((i) => s"test$i")

  def findNewName:String = supply.take(1).runLast.run.get

  def freeJdbcUrl = s"jdbc:h2:mem:$findNewName;DB_CLOSE_DELAY=-1"



}
