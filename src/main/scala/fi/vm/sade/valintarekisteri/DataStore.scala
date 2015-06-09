package fi.vm.sade.valintarekisteri

import scalaz.stream._
import scalaz.concurrent.Task

trait DataStore[Item, Query] {

  val newItems: Sink[Task, Item]

  val henkiloQuery: Channel[Task, Query, Process[Task, Item]]

}

