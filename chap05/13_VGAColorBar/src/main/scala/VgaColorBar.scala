// See LICENSE for license details.

import chisel3._
import chisel3.util._

class VgaBundle extends Bundle {
  val red = UInt(4.W)
  val green = UInt(4.W)
  val blue = UInt(4.W)
  val hSync = Bool()
  val vSync = Bool()
}

object VGA {
  val fps         = 60  // Scan 60 times per 1 sec.
  
  val hMax        = 800 // The num of horizontal pixcels (include non-display)
  val hSyncPeriod = 96  // Period of horizontal sync siginal
  val hBackPorch  = 48  // Back poarch of horizontal sync signal
  val hFrontPorch = 16  // Front poarch of horizontal sync signal
  
  val vMax        = 521 // The num of vertical lines (include non-display)
  val vSyncPeriod = 2   // Period of vertical sync siginal
  val vBackPorch  = 33  // Back poarch of vertical sync signal
  val vFrontPorch = 10  // Front poarch of vertical sync signal

  // If the image is shifted vertically, set the following
  // val vMax        = 512
  // val vSyncPeriod = 2
  // val vBackPorch  = 10
  // val vFrontPorch = 20

  // Number of clocks for one pixel(Exactly 3.9987, but almost 4.)
  val pxClock = (100000000.0 / fps / vMax / hMax).round.toInt 
}

import VGA._

class VgaColorBar extends Module {
  val io = IO(new Bundle {
    val vga = Output(new VgaBundle)
  })

  val (pxCount, pxEn) = Counter(true.B, pxClock)
  val (hCount, hEn)   = Counter(pxEn, hMax)
  val (vCount, vEn)   = Counter(hEn, vMax)

  // Whether it is a display pixel
  val pxEnable = (hSyncPeriod + hBackPorch).U <= hCount && hCount < (hMax - hFrontPorch).U &&
    (vSyncPeriod + vBackPorch).U <= vCount && vCount < (vMax - vFrontPorch).U

  // 表示ピクセルかどうか
  val colorNum = (hCount - (hSyncPeriod + hBackPorch).U) / (640 / 8).U
  val color = MuxCase("h000000".U,
    Array(
      (colorNum === 0.U) -> "hfff".U, // White
      (colorNum === 1.U) -> "h000".U, // Black
      (colorNum === 2.U) -> "hff0".U, // Yellow
      (colorNum === 3.U) -> "h0ff".U, // Cyan
      (colorNum === 4.U) -> "h0f0".U, // Green
      (colorNum === 5.U) -> "hf0f".U, // Magenta
      (colorNum === 6.U) -> "hf00".U, // Red
      (colorNum === 7.U) -> "h00f".U  // Blue
    ))

  val pxColor = RegNext(Mux(pxEnable, color, "h000".U))
  io.vga.red   := pxColor(11, 8)
  io.vga.green := pxColor(7, 4)
  io.vga.blue  := pxColor(3, 0)

  io.vga.hSync := RegNext(!(hCount < hSyncPeriod.U))
  io.vga.vSync := RegNext(!(vCount < vSyncPeriod.U))
}

object VgaColorBar extends App {
  chisel3.Driver.execute(args, () => new VgaColorBar)
}
