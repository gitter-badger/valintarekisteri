package fi.vm.sade.valintarekisteri

import scalaz.stream._
import scalaz.concurrent.Task

trait DataStore {

  val newItems: Sink[Task, VastaanottoTieto]

  val henkiloQuery: Channel[Task, String, Seq[VastaanottoTieto]]

}

