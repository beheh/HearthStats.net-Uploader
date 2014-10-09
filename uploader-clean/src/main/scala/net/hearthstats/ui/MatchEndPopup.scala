package net.hearthstats.ui

import java.awt.{ Color, Component, Dimension, FlowLayout, Font, Insets }
import java.awt.event.{ ActionEvent, KeyAdapter, KeyEvent }

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.ListBuffer

import org.apache.commons.lang3.StringUtils

import net.hearthstats.util.SwingHelper._

import javax.swing._
import javax.swing.event.{ ChangeEvent, ChangeListener }
import net.hearthstats.core.{ GameMode, HearthstoneMatch, HeroClass, MatchOutcome, Rank }
import net.hearthstats.hstatsapi.DeckUtils
import net.hearthstats.util.Translation
import net.miginfocom.swing.MigLayout

/**
 * A popup to display at the end of the match that allows the match details to
 * be edited.
 *
 * @author gtch
 * @author tyrcho
 */
class MatchEndPopup(
  translation: Translation,
  deckUtils: DeckUtils) {

  import translation._

  def showPopup(parentComponent: Component, hsMatch: HearthstoneMatch): Button = {
    val popup = new MatchEndPopupImpl(hsMatch)
    val value = JOptionPane.showOptionDialog(parentComponent, popup, "Incomplete match detected", JOptionPane.INFORMATION_MESSAGE,
      JOptionPane.YES_NO_OPTION, null, Array("Submit", "Cancel"), "Submit")
    value match {
      case 0 => Button.SUBMIT
      case 1 => Button.CANCEL
      case _ => Button.CANCEL
    }
  }

  private def setDefaultSize(c: Component) {
    c.setMinimumSize(new Dimension(180, 27))
    c.setPreferredSize(new Dimension(200, 28))
  }

  class MatchEndPopupImpl(hsMatch: HearthstoneMatch)
    extends JPanel {

    setLayout(new MigLayout("", "[]10[grow]20[]10[grow]", ""))
    val rankPanel = new JPanel
    val deckPanel = new JPanel
    val undetectedLabel = "- " + t("undetected") + " -"
    val errorMessages = determineErrors(hsMatch)

    val preferredHeight = 380 + (30 * errorMessages.size)
    setMinimumSize(new Dimension(660, 380))
    setPreferredSize(new Dimension(660, preferredHeight))
    setMaximumSize(new Dimension(660, preferredHeight + 200))
    val heading = new JLabel(if (hsMatch.mode == null) t("match.popup.heading") else hsMatch.mode + " " + t("match.popup.heading"))
    val headingFont = heading.getFont.deriveFont(20f)
    heading.setFont(headingFont)
    add(heading, "span")
    if (errorMessages.nonEmpty) {
      val errorLabel = new JLabel(errorMessages.mkString("<html>- ", "<br>- ", "</html>"))
      errorLabel.setForeground(Color.RED.darker)
      add(errorLabel, "span, gapy 5px 10px")
    }
    add(new JLabel(t("match.label.game_mode")), "right")

    val gameModeComboBox = new JComboBox(GameMode.values)
    setDefaultSize(gameModeComboBox)
    if (hsMatch.mode != null) {
      gameModeComboBox.setSelectedItem(hsMatch.mode)
    }
    gameModeComboBox.addActionListener(() => {
      hsMatch.mode = gameModeComboBox.getItemAt(gameModeComboBox.getSelectedIndex)
      updateGameMode()
    })
    add(gameModeComboBox, "span")

    add(new JLabel(t("match.label.your_rank")), "right")
    val rankComboBox = new JComboBox(Rank.values)
    setDefaultSize(rankComboBox)
    rankComboBox.addActionListener(() =>
      hsMatch.rankLevel = rankComboBox.getSelectedItem.asInstanceOf[Rank])
    if (hsMatch.rankLevel != null) {
      rankComboBox.setSelectedItem(hsMatch.rankLevel)
    }

    add(rankPanel, "")
    add(new JLabel(t("match.label.opponent_name")), "right")
    val opponentNameField = new JTextField
    setDefaultSize(opponentNameField)
    opponentNameField.setText(hsMatch.opponentName)
    opponentNameField.addKeyListener(new KeyAdapter {
      override def keyReleased(e: KeyEvent) {
        hsMatch.opponentName = opponentNameField.getText.trim
      }
    })
    add(opponentNameField, "wrap")

    add(new JLabel(t("match.label.your_class")), "right")
    val yourClassComboBox = new JComboBox(HeroClass.values)
    setDefaultSize(yourClassComboBox)
    yourClassComboBox.setSelectedItem(hsMatch.userClass)
    yourClassComboBox.addActionListener(() =>
      hsMatch.userClass = HeroClass.values()(yourClassComboBox.getSelectedIndex))
    add(yourClassComboBox, "")

    add(new JLabel(t("match.label.opponents_class")), "right")
    val opponentClassComboBox = new JComboBox(HeroClass.values)
    setDefaultSize(opponentClassComboBox)
    opponentClassComboBox.setSelectedItem(hsMatch.opponentClass)
    opponentClassComboBox.addActionListener(() =>
      hsMatch.opponentClass = HeroClass.values()(opponentClassComboBox.getSelectedIndex))
    add(opponentClassComboBox, "wrap")
    add(new JLabel(t("match.label.your_deck")), "right")
    val deckSlotList = Array.ofDim[String](10)
    deckSlotList(0) = undetectedLabel
    var i = 1
    while (i <= 9) {
      val deck = deckUtils.getDeckFromSlot(i)
      val sb = new StringBuilder()
      sb.append(t("deck_slot.label", i))
      sb.append(" ")
      if (deck.isEmpty) {
        sb.append(t("undetected"))
      } else {
        sb.append(deck.get.name)
      }
      deckSlotList(i) = sb.toString
      i += 1
    }
    val yourDeckComboBox = new JComboBox(deckSlotList)
    setDefaultSize(yourDeckComboBox)
    yourDeckComboBox.setSelectedIndex(hsMatch.deckSlot)
    yourDeckComboBox.addActionListener(() => hsMatch.deckSlot = yourDeckComboBox.getSelectedIndex)
    add(deckPanel, "wrap")

    add(new JLabel(t("match.label.coin")), "right")
    val coinCheckBox = new JCheckBox(t("match.coin"))
    coinCheckBox.setSelected(hsMatch.coin.getOrElse(false))
    coinCheckBox.addChangeListener(() => hsMatch.coin = Some(coinCheckBox.isSelected))
    add(coinCheckBox, "wrap")

    add(new JLabel("Result:"), "right, gapy 20px 20px")
    val resultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1))
    val resultVictory = new JRadioButton(t("match.label.result_victory"))
    resultVictory.setMnemonic(KeyEvent.VK_V)
    resultVictory.setMargin(new Insets(0, 0, 0, 10))
    if (hsMatch.result == Some(MatchOutcome.VICTORY)) {
      resultVictory.setSelected(true)
    }
    resultVictory.addActionListener(() =>
      if (resultVictory.isSelected) { hsMatch.result = Some(MatchOutcome.VICTORY) })
    resultPanel.add(resultVictory)
    val resultDefeat = new JRadioButton(t("match.label.result_defeat"))
    resultDefeat.setMnemonic(KeyEvent.VK_D)
    resultDefeat.setMargin(new Insets(0, 0, 0, 10))
    if (Some(MatchOutcome.DEFEAT) == hsMatch.result) {
      resultDefeat.setSelected(true)
    }
    resultDefeat.addActionListener(() =>
      if (resultDefeat.isSelected) { hsMatch.result = Some(MatchOutcome.DEFEAT) })
    resultPanel.add(resultDefeat)
    val resultDraw = new JRadioButton(t("match.label.result_draw"))
    resultDraw.setMnemonic(KeyEvent.VK_R)
    resultDraw.setMargin(new Insets(0, 0, 0, 10))
    if ("Draw" == hsMatch.result) {
      resultDraw.setSelected(true)
    }
    resultDraw.addActionListener(() =>
      if (resultDraw.isSelected) { hsMatch.result = Some(MatchOutcome.DRAW) })
    resultPanel.add(resultDraw)
    val resultGroup = new ButtonGroup
    resultGroup.add(resultVictory)
    resultGroup.add(resultDefeat)
    resultGroup.add(resultDraw)
    add(resultPanel, "span 3, wrap")

    add(new JLabel(t("match.label.notes")), "right")
    val notesTextArea = new JTextArea
    notesTextArea.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black),
      BorderFactory.createEmptyBorder(3, 6, 3, 6)))
    notesTextArea.setMinimumSize(new Dimension(550, 100))
    notesTextArea.setPreferredSize(new Dimension(550, 150))
    notesTextArea.setMaximumSize(new Dimension(550, 200))
    notesTextArea.setBackground(Color.WHITE)
    notesTextArea.setText(hsMatch.notes)
    notesTextArea.addKeyListener(new KeyAdapter {
      override def keyReleased(e: KeyEvent) {
        hsMatch.notes = notesTextArea.getText
      }
    })
    add(notesTextArea, "span 3, wrap")

    updateGameMode()

    private def updateGameMode() {
      val isRanked = hsMatch.mode == GameMode.RANKED
      rankPanel.removeAll()
      if (isRanked) {
        if (hsMatch.rankLevel != null) {
          rankComboBox.setSelectedIndex(25 - hsMatch.rankLevel.number)
        }
        rankPanel.add(rankComboBox)
      } else {
        val rankMessage = hsMatch.mode match {
          case GameMode.ARENA => "N/A: Arena Mode"
          case GameMode.CASUAL => "N/A: Casual Mode"
          case _ => "N/A"
        }
        val rankNotApplicable = new JLabel(rankMessage)
        rankNotApplicable.setFont(rankNotApplicable.getFont.deriveFont(Font.ITALIC))
        rankNotApplicable.setEnabled(false)
        setDefaultSize(rankNotApplicable)
        rankPanel.add(rankNotApplicable)
      }
      deckPanel.removeAll()
      if (GameMode.ARENA != hsMatch.mode) {
        deckPanel.add(yourDeckComboBox)
      } else {
        val deckNotApplicable = new JLabel("N/A: Arena Mode")
        deckNotApplicable.setFont(deckNotApplicable.getFont.deriveFont(Font.ITALIC))
        deckNotApplicable.setEnabled(false)
        setDefaultSize(deckNotApplicable)
        deckPanel.add(deckNotApplicable)
      }
      validate()
      repaint()
    }

    private def determineErrors(hsMatch: HearthstoneMatch) = {
      val result = ListBuffer.empty[String]
      if (hsMatch.mode == null) {
        result += t("match.popup.error.mode")
      }
      if (hsMatch.rankLevel == null && GameMode.RANKED == hsMatch.mode) {
        result += t("match.popup.error.rank")
      }
      if (hsMatch.userClass == HeroClass.UNDETECTED) {
        result += t("match.popup.error.yourclass")
      }
      if (StringUtils.isBlank(hsMatch.opponentName)) {
        result += t("match.popup.error.opponentname")
      }
      if (hsMatch.opponentClass == HeroClass.UNDETECTED) {
        result += t("match.popup.error.opponentclass")
      }
      if (hsMatch.result.isEmpty) {
        result += t("match.popup.error.result")
      }
      result
    }
  }
}