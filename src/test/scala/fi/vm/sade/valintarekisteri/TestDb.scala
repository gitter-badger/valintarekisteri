package fi.vm.sade.valintarekisteri

import scalaz.stream._
import scalaz.stream.async.mutable.Signal
import scala.annotation.tailrec


object TestDb {


  val s:Signal[List[String]] = async.signalOf(List[String]())

  @tailrec def findNewName(names:List[String], index:Int = 1):String = {
    val newName = s"test$index"
    if (names.contains(newName)) findNewName(names, index + 1)
    else newName
  }

  def getNew = s.compareAndSet{
    case None => Some(List("test"))
    case Some(names) => Some(findNewName(names) +: names)

  }.run.get.head


  def freeJdbcUrl = s"jdbc:h2:mem:$getNew;DB_CLOSE_DELAY=-1"



}
