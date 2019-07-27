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

abstract class DispMode {
  val hMax: Int        // The num of horizontal pixcels (include non-display)
  val hSyncPeriod: Int // Period of horizontal sync siginal
  val hBackPorch: Int  // Back poarch of horizontal sync signal
  val hFrontPorch: Int // Front poarch of horizontal sync signal

  val vMax: Int        // The num of vertical lines (include non-display)
  val vSyncPeriod: Int // Period of vertical sync siginal
  val vBackPorch: Int  // Back poarch of vertical sync signal
  val vFrontPorch: Int // Front poarch of vertical sync signal

  // num of horizontal diplay pixcel
  def hDispMax = hMax - (hSyncPeriod + hBackPorch + hFrontPorch)
  // num of vertical diplay pixcel
  def vDispMax = vMax - (vSyncPeriod + vBackPorch + vFrontPorch) 
  // num of whole screen display pixcel
  def pxMax = hDispMax * vDispMax

  // num of lines in screen (one character height is 16 px)
  def row = vDispMax / 16
  // num of characters in line (one characters width is 8 px)
  def col = hDispMax / 8
}

object VGA extends DispMode {
  val hMax        = 800
  val hSyncPeriod = 96
  val hBackPorch  = 48
  val hFrontPorch = 16

  val vMax        = 521
  val vSyncPeriod = 2
  val vBackPorch  = 33
  val vFrontPorch = 10
  // If the image is shifted vertically, set the following
  // val vMax        = 512
  // val vSyncPeriod = 2
  // val vBackPorch  = 10
  // val vFrontPorch = 20
}

object SVGA extends DispMode {
  val hMax        = 1056
  val hSyncPeriod = 128
  val hBackPorch  = 88
  val hFrontPorch = 40

  val vMax        = 628
  val vSyncPeriod = 4
  val vBackPorch  = 23
  val vFrontPorch = 1
}

class VramDataBundle extends Bundle {
  val addr = UInt(19.W)
  val data = UInt(8.W) // Red: 7〜5, Green: 4〜2, Blue:1〜0 bit
}

class VgaDisplay(mode: DispMode) extends Module {
  val io = IO(new Bundle {
    val vramData = Flipped(Decoupled(new VramDataBundle))
    val vramClock = Input(Clock())
    val vga = Output(new VgaBundle)
  })

  // Count up every screen clock
  val (hCount, hEn)   = Counter(true.B, mode.hMax)
  val (vCount, vEn)   = Counter(hEn, mode.vMax)

  // Whether it is a display pixel
  val pxEnable =
    (mode.hSyncPeriod + mode.hBackPorch).U <= hCount && hCount < (mode.hMax - mode.hFrontPorch).U &&
    (mode.vSyncPeriod + mode.vBackPorch).U <= vCount && vCount < (mode.vMax - mode.vFrontPorch).U

  val (vramAddr, wrap) = Counter(pxEnable, mode.pxMax)
  when (hCount === 0.U && vCount === 0.U) {
    vramAddr := 0.U
  }

  val vram = Module(new Vram)
  // A port is for external writing
  vram.io.clka := io.vramClock
  vram.io.ena := io.vramData.valid
  vram.io.wea := io.vramData.valid
  vram.io.addra := io.vramData.bits.addr
  vram.io.dina := io.vramData.bits.data
  io.vramData.ready := true.B

  // Read from B port and output to VGA port
  vram.io.clkb := clock
  vram.io.enb := pxEnable
  vram.io.web := false.B
  vram.io.addrb := vramAddr
  vram.io.dinb := 0.U
  // Since the output from VRAM is delayed by 1 Clock, pxEnable is received by RegNext
  val pxData = Mux(RegNext(pxEnable, false.B), vram.io.doutb, 0.U) 

  io.vga.red   := Cat(pxData(7, 5), pxData(5))
  io.vga.green := Cat(pxData(4, 2), pxData(2))
  io.vga.blue  := Cat(pxData(1, 0), pxData(0), pxData(0))

  // Align to delay of output from VRAM
  io.vga.hSync := RegNext(!(hCount < mode.hSyncPeriod.U), true.B)
  io.vga.vSync := RegNext(!(vCount < mode.vSyncPeriod.U), true.B)
}
