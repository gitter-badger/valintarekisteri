package fi.vm.sade.valintarekisteri

import scalaz.stream.Process
import scodec.bits.ByteVector
import scalaz.stream.Process.Halt
import scalaz.stream.Cause.Error


trait ResponseSupport extends Decoders {

  val decodeUtf8 = Process.receive1[ByteVector, String] {
    (bv: ByteVector) => bv.decodeUtf8 match {
      case Left(e) => Halt(Error(e))
      case Right(bs) => Process.emit(bs)
    }

  }



}
