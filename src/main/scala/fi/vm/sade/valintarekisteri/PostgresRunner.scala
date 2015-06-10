package fi.vm.sade.valintarekisteri

import java.io.File
import java.nio.file.{Path, Files}

object PostgresRunner extends App {
  new PostgresRunner("postgresql/data").start
}

class PostgresRunner(dataDirName: String, port: Integer = 5432) {
  import sys.process._

  lazy val dataPath = new File(dataDirName).toPath

  private var serverProcess: Option[Process] = None

  private def ensureDataDirExists = {
    if (!dataDirExists) {
      createDataDir
    } else {
      println("Data directory exists")
    }
  }

  private def createDataDir = {
    println("Initializing data directory")
    Files.createDirectory(dataPath)
    s"chmod 0700 $dataDirName" !;
    s"initdb -D $dataDirName" !;
  }

  def start = if (!serverProcess.isDefined) {
    ensureDataDirExists
    println("Starting server on port " + port)
    serverProcess = Some(("postgres --config_file=postgresql/postgresql.conf -D " + dataDirName + " -p " + port).run)

  }


  def stop = {
    serverProcess.foreach(_.destroy())
    serverProcess = None
  }

  private def dataDirExists = Files.exists(dataPath)
}