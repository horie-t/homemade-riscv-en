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

  val hDispMax = hMax - (hSyncPeriod + hBackPorch + hFrontPorch)
  val vDispMax = vMax - (vSyncPeriod + vBackPorch + vFrontPorch)

  // Number of clocks for one pixel(Exactly 3.9987, but almost 4.)
  val pxClock = (100000000.0 / fps / vMax / hMax).round.toInt 
  val pxMax = hDispMax * vDispMax
}

class VramDataBundle extends Bundle {
  val addr = UInt(19.W)
  val data = UInt(8.W) // Red: 7〜5, Green: 4〜2, Blue:1〜0 bit
}

import VGA._

class VgaDisplay extends Module {
  val io = IO(new Bundle {
    val vramData = Flipped(Decoupled(new VramDataBundle))
    val vga = Output(new VgaBundle)
  })

  val (pxCount, pxEn) = Counter(true.B, pxClock)
  val (hCount, hEn)   = Counter(pxEn, hMax)
  val (vCount, vEn)   = Counter(hEn, vMax)

  // Whether it is a display pixel
  val pxEnable = (hSyncPeriod + hBackPorch).U <= hCount && hCount < (hMax - hFrontPorch).U &&
    (vSyncPeriod + vBackPorch).U <= vCount && vCount < (vMax - vFrontPorch).U

  val (vramAddr, wrap) = Counter(pxEn && pxEnable, pxMax)
  when (hCount === 0.U && vCount === 0.U) {
    vramAddr := 0.U
  }

  val vram = Module(new Vram)
  // A port is for external writing
  vram.io.clka := clock
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
  io.vga.hSync := RegNext(!(hCount < hSyncPeriod.U), true.B)
  io.vga.vSync := RegNext(!(vCount < vSyncPeriod.U), true.B)
}
