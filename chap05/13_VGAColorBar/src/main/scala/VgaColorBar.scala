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

  val pxClock = (100000000.0 / 60 / 521 / 800).round.toInt // 1ピクセル分のクロック数
}

import VGA._

class VgaColorBar extends Module {
  val io = IO(new Bundle {
    val vga = Output(new VgaBundle)
  })

  val (pxCount, pxEn) = Counter(true.B, pxClock)
  val (hCount, hEn)   = Counter(pxEn, hMax)
  val (vCount, vEn)   = Counter(hEn, vMax)

  // 表示ピクセルかどうか
  val pxEnable = (hSyncPeriod + hBackPorch).U <= hCount && hCount < (hMax - hFrontPorch).U &&
    (vSyncPeriod + vBackPorch).U <= vCount && vCount < (vMax - vFrontPorch).U

  // 640ピクセルを8つの領域に分割する。
  val colorNum = (hCount - (hSyncPeriod + hBackPorch).U) / (640 / 8).U
  val color = MuxCase("h000000".U,
    Array(
      (colorNum === 0.U) -> "hfff".U, // 白
      (colorNum === 1.U) -> "h000".U, // 黒
      (colorNum === 2.U) -> "hff0".U, // 黄
      (colorNum === 3.U) -> "h0ff".U, // シアン
      (colorNum === 4.U) -> "h0f0".U, // 緑
      (colorNum === 5.U) -> "hf0f".U, // マゼンタ
      (colorNum === 6.U) -> "hf00".U, // 赤
      (colorNum === 7.U) -> "h00f".U  // 青
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
