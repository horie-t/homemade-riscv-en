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
  val hMax: Int        // 水平方向のピクセル数(非表示期間も含む)
  val hSyncPeriod: Int // 水平同期の期間
  val hBackPorch: Int  // 水平バックポーチ
  val hFrontPorch: Int // 水平フロントポーチ

  val vMax: Int        // 垂直方向のライン数(非表示期間も含む)
  val vSyncPeriod: Int // 垂直同期の期間
  val vBackPorch: Int  // 垂直バックポーチ
  val vFrontPorch: Int // 垂直フロントポーチ

  def hDispMax = hMax - (hSyncPeriod + hBackPorch + hFrontPorch) // 水平方向の表示ピクセル数
  def vDispMax = vMax - (vSyncPeriod + vBackPorch + vFrontPorch) // 垂直方向の表示ピクセル数
  def pxMax = hDispMax * vDispMax                                // 画面全体の表示ピクセル数

  def row = vDispMax / 16 // 文字の行数(1文字の高さを16ピクセルとする)
  def col = hDispMax / 8 // 文字の行数(1文字の幅を8ピクセルとする)
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
  // 仕様上は上記が正しいが、下記にしないとずれるモニタもある。
  // val vMax        = 512 // 垂直方向のライン数(非表示期間も含む)
  // val vSyncPeriod = 2   // 垂直同期の期間
  // val vBackPorch  = 10  // 垂直バックポーチ
  // val vFrontPorch = 20  // 垂直フロントポーチ
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
  val data = UInt(8.W) // Red: 7〜5, Green: 4〜2, Blue:1〜0ビットを割り当て。
}

class VgaDisplay(mode: DispMode) extends Module {
  val io = IO(new Bundle {
    val vramData = Flipped(Decoupled(new VramDataBundle))
    val vramClock = Input(Clock())
    val vga = Output(new VgaBundle)
  })

  val (hCount, hEn)   = Counter(true.B, mode.hMax) // 画面用のクロック毎にカウント・アップ
  val (vCount, vEn)   = Counter(hEn, mode.vMax)

  // 表示ピクセルかどうか
  val pxEnable =
    (mode.hSyncPeriod + mode.hBackPorch).U <= hCount && hCount < (mode.hMax - mode.hFrontPorch).U &&
    (mode.vSyncPeriod + mode.vBackPorch).U <= vCount && vCount < (mode.vMax - mode.vFrontPorch).U

  val (vramAddr, wrap) = Counter(pxEnable, mode.pxMax)
  when (hCount === 0.U && vCount === 0.U) {
    vramAddr := 0.U
  }

  val vram = Module(new Vram)
  // Aポートは外部からの書き込み用
  vram.io.clka := io.vramClock
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
  io.vga.hSync := RegNext(!(hCount < mode.hSyncPeriod.U), true.B)
  io.vga.vSync := RegNext(!(vCount < mode.vSyncPeriod.U), true.B)
}
