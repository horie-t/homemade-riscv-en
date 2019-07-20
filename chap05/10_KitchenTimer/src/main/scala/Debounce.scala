// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** プッシュボタン用デバンウンス
  */
class Debounce extends Module {
  val io = IO(new Bundle{
    val in = Input(Bool())
    val out = Output(Bool())
  })

  val (count, enable) = Counter(true.B, 10000000) // 100ミリ秒間隔で値を取り込む

  val reg0 = RegEnable(io.in, false.B, enable)
  val reg1 = RegEnable(reg0,  false.B, enable)

  io.out := reg0 && !reg1 && enable // enableの時だけ変化を見るようにして、1クロックのパルスにする
}

/** プッシュボタン用デバンウンス
  */
object Debounce {
  def apply(in: Bool): Bool = {
    val debounce = Module(new Debounce)
    debounce.io.in := in
    debounce.io.out
  }
}
