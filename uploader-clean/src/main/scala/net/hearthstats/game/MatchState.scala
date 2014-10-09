package net.hearthstats.game

import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.core.Rank
import net.hearthstats.core.GameMode

class MatchState {
  var currentMatch: Option[HearthstoneMatch] = None
  var lastMatch: Option[HearthstoneMatch] = None
  var rankLevel: Rank = _
  var startTime: Long = _
  var submitted = false

  def setOpponentName(n: String) = currentMatch.get.opponentName = n
  def setNotes(n: String) = currentMatch.get.notes = n
  def setCoin(c: Boolean) = currentMatch.get.coin = Some(c)

  def lastMatchUrl: Option[String] =
    for (m <- lastMatch) yield {
      if ("Arena" == m.mode) "http://hearthstats.net/arenas/new"
      else m.editUrl
    }

  def nextMatch(mode: GameMode): Unit = {
    lastMatch = currentMatch
    currentMatch = Some(new HearthstoneMatch(mode = mode))
    submitted = false
  }

}