package net.hearthstats.game

import scala.concurrent.duration.DurationInt
import scala.collection.JavaConversions._

object LogParserMain extends App {
  val p = new LogParser
  val lines = io.Source.fromFile("""C:\Utilisateurs\a518291\Dropbox\Public\hearthstats\output_log.txt""").getLines

  for {
    l <- lines
    r <- p.analyseLine(l)
    if r.toString != ""
  } {
    println(l)
    println(r)
  }
}

