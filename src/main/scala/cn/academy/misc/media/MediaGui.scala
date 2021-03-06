package cn.academy.misc.media

import cn.academy.core.Resources
import cn.academy.core.client.ui.ACHud
import cn.academy.core.client.ui.ACHud.Condition
import cn.academy.misc.media.MediaBackend.PlayInfo
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.cgui.gui.component.TextBox.ConfirmInputEvent
import cn.lambdalib.cgui.gui.component.VerticalDragBar.DraggedEvent
import cn.lambdalib.cgui.gui.component._
import cn.lambdalib.cgui.gui.event.{FrameEvent, LeftClickEvent, LostFocusEvent}
import cn.lambdalib.cgui.gui.{CGuiScreen, Widget, WidgetContainer}
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.util.client.auxgui.AuxGui
import cn.lambdalib.util.helper.{Color, GameTimer}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft

private object MediaGuiInit {

  lazy val document: WidgetContainer = CGUIDocument.panicRead(Resources.getGui("media_player"))

}

/**
  * @author WeAthFolD
  */
class MediaGui extends CGuiScreen {
  import MediaGuiInit._
  import cn.lambdalib.cgui.ScalaCGUI._

  val backend = MediaBackend(thePlayer)

  val T_PLAY = Resources.getTexture("guis/apps/media_player/play")
  val T_PAUSE = Resources.getTexture("guis/apps/media_player/pause")

  val pageMain = document.getWidget("back").copy

  val data = MediaAcquireData(thePlayer)
  val allInstalled = MediaManager.allMedias.filter(data.isInstalled)

  { // Init media elements
    val area = pageMain.child("area")
    val list = new ElementList

    allInstalled.foreach(media => {
      val ret = document.getWidget("back/t_one").copy
      ret.transform.doesDraw = true

      ret.child("icon").component[DrawTexture].texture = media.cover
      ret.child("title").component[TextBox].content = media.name
      ret.child("desc").component[TextBox].content = media.desc
      ret.child("time").component[TextBox].content = media.displayLength

      ret.listens[LeftClickEvent](() => {
        backend.play(media)
        updatePlayDisplay()
      })

      if (media.external) {
        wrapEdit(ret.child("btn_edit_name"), ret.child("title"), newName => media.propExternalName().set(newName))
        wrapEdit(ret.child("btn_edit_desc"), ret.child("desc"), newDesc => media.propExternalDesc().set(newDesc))
      }

      list.addWidget(ret)
    })

    area :+ list
  }

  { // Update every 500ms
    var lastTest = GameTimer.getTime
    pageMain.listens[FrameEvent](() => {
      val time = GameTimer.getTime
      if (time - lastTest > 500) {
        lastTest = time
        updatePlayDisplay()
      }
    })
  }

  // Scroll bar
  pageMain.child("scroll_bar").listens((w, evt: DraggedEvent) => {
    val vdb = w.component[VerticalDragBar]
    val list = pageMain.child("area").component[ElementList]
    list.setProgress((vdb.getProgress * list.getMaxProgress).toInt)
  })

  // Play or pause button
  pageMain.child("pop").listens[LeftClickEvent](() => {
    currentPlaying match {
      case Some(PlayInfo(_, false, _)) =>
        backend.pauseCurrent()
        updatePlayDisplay()
      case Some(PlayInfo(_, true, _)) =>
        backend.continueCurrent()
        updatePlayDisplay()
      case _ => backend.lastPlayed match {
        case Some(media) => backend.play(media); updatePlayDisplay()
        case _ =>
          if (allInstalled.nonEmpty) {
            backend.play(allInstalled.head); updatePlayDisplay()
          }
      }
    }

    updatePlayDisplay()
  })

  // Stop button
  pageMain.child("stop").listens[LeftClickEvent](() => {
    backend.stopCurrent()
    updatePlayDisplay()
  })

  // Volume editing
  pageMain.child("volume_bar").component[DragBar].setProgress(backend.getVolume)

  pageMain.child("volume_bar").listens((w, evt: DragBar.DraggedEvent) => {
    val bar = w.component[DragBar]
    backend.setVolume(bar.getProgress.toFloat)
  })

  getGui.addWidget("main", pageMain)

  updatePlayDisplay()

  def wrapEdit(button: Widget, box: Widget, callback: String => Any) = {
    button.transform.doesDraw = true

    val dt = new DrawTexture(null).setColor(Color.monoBlend(.4, 0))
    box :+ dt

    val textBox = box.component[TextBox]

    var canEdit = false
    button.listens[LeftClickEvent](() => {
      if (!canEdit) {
        canEdit = true
        dt.color.a =   .2
        textBox.allowEdit = true
        box.gainFocus()
        box.transform.doesListenKey = true
      }
    })

    val confirmHandler = () => {
      if (canEdit) {
        canEdit = false
        textBox.allowEdit = false
        box.transform.doesListenKey = false
        dt.color.a = 0
        callback(textBox.content)
      }
    }

    box.listens[ConfirmInputEvent](confirmHandler)
    box.listens[LostFocusEvent](confirmHandler)
  }

  /**
    * Fetches current playing info and updates it to all GUI elements.
    */
  def updatePlayDisplay() = {
    val playTimeText = pageMain.child("play_time").component[TextBox]
    val progress = pageMain.child("progress").component[ProgressBar]
    val title = pageMain.child("title").component[TextBox]
    val popTexture = pageMain.child("pop").component[DrawTexture]

    currentPlaying match {
      case Some(a @ PlayInfo(media, paused, time)) =>
        playTimeText.setContent(a.displayTime)
        progress.progress = time / media.lengthSecs
        title.setContent(media.name)
        popTexture.setTex(if (paused) T_PLAY else T_PAUSE)
      case _ =>
        playTimeText.setContent("00:00")
        progress.progress = 0
        title.setContent("")
        popTexture.setTex(T_PLAY)
    }
  }

  def currentPlaying = backend.currentPlaying

  def thePlayer = Minecraft.getMinecraft.thePlayer

  override def doesGuiPauseGame(): Boolean = false
}

@SideOnly(Side.CLIENT)
@Registrant
private object MediaAuxGui {
  import cn.lambdalib.cgui.ScalaCGUI._

  @RegInitCallback
  def init() = {
    val base = CGUIDocument.panicRead(Resources.getGui("media_player_aux")).getWidget("base")

    ACHud.instance.addElement(base, new Condition {
      override def shows(): Boolean = MediaBackend().currentPlaying.isDefined
    }, "media", base.copy)

    val progress = base.child("progress").component[ProgressBar]
    val title = base.child("title").component[TextBox]
    val timetext  = base.child("time").component[TextBox]

    var lastTest = GameTimer.getTime
    base.listens[FrameEvent](() => {
      val time = GameTimer.getTime
      if (time - lastTest > 500) {
        lastTest = time

        val backend = MediaBackend()
        backend.currentPlaying match {
          case Some(a @ PlayInfo(media, paused, playedTime)) =>
            progress.progress = playedTime / media.lengthSecs
            title.setContent(media.name)
            timetext.setContent(a.displayTime)
          case _ =>
            progress.progress = 0
            title.setContent("")
            timetext.setContent("")
        }
      }
    })
  }

}