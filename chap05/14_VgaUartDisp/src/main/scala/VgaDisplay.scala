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
  val fps         = 60  // 1秒間に60回画面全体を描画

  val hMax        = 800 // 水平方向のピクセル数(非表示期間も含む)
  val hSyncPeriod = 96  // 水平同期の期間
  val hBackPorch  = 48  // 水平バックポーチ
  val hFrontPorch = 16  // 水平フロントポーチ

  val vMax        = 521 // 垂直方向のライン数(非表示期間も含む)
  val vSyncPeriod = 2   // 垂直同期の期間
  val vBackPorch  = 33  // 垂直バックポーチ
  val vFrontPorch = 10  // 垂直フロントポーチ
  // 仕様上は上記が正しいが、下記にしないとずれるモニタもある。
  // val vMax        = 512 // 垂直方向のライン数(非表示期間も含む)
  // val vSyncPeriod = 2   // 垂直同期の期間
  // val vBackPorch  = 10  // 垂直バックポーチ
  // val vFrontPorch = 20  // 垂直フロントポーチ

  val hDispMax = hMax - (hSyncPeriod + hBackPorch + hFrontPorch)
  val vDispMax = vMax - (vSyncPeriod + vBackPorch + vFrontPorch)

  val pxClock = (100000000.0 / fps / vMax / hMax).round.toInt // 1ピクセル分のクロック数
  val pxMax = hDispMax * vDispMax
}

class VramDataBundle extends Bundle {
  val addr = UInt(19.W)
  val data = UInt(8.W) // Red: 7〜5, Green: 4〜2, Blue:1〜0ビットを割り当て。
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

  // 表示ピクセルかどうか
  val pxEnable = (hSyncPeriod + hBackPorch).U <= hCount && hCount < (hMax - hFrontPorch).U &&
    (vSyncPeriod + vBackPorch).U <= vCount && vCount < (vMax - vFrontPorch).U

  val (vramAddr, wrap) = Counter(pxEn && pxEnable, pxMax)
  when (hCount === 0.U && vCount === 0.U) {
    vramAddr := 0.U
  }

  val vram = Module(new Vram)
  // Aポートは外部からの書き込み用
  vram.io.clka := clock
  vram.io.ena := io.vramData.valid
  vram.io.wea := io.vramData.valid
  vram.io.addra := io.vramData.bits.addr
  vram.io.dina := io.vramData.bits.data
  io.vramData.ready := true.B

  // Bポートから読み出して、VGAポートに出力
  vram.io.clkb := clock
  vram.io.enb := pxEnable
  vram.io.web := false.B
  vram.io.addrb := vramAddr
  vram.io.dinb := 0.U
  // VRAMからの出力は1Clock遅れるので、pxEnableをRegNextで受けている
  val pxData = Mux(RegNext(pxEnable, false.B), vram.io.doutb, 0.U) 

  io.vga.red   := Cat(pxData(7, 5), pxData(5))
  io.vga.green := Cat(pxData(4, 2), pxData(2))
  io.vga.blue  := Cat(pxData(1, 0), pxData(0), pxData(0))

  // VRAMからの出力の遅れに合わせる
  io.vga.hSync := RegNext(!(hCount < hSyncPeriod.U), true.B)
  io.vga.vSync := RegNext(!(vCount < vSyncPeriod.U), true.B)
}
