// See LICENSE for license details.

import chisel3._

/** マルチプレクサ。
  * selectorが0ならin_0を、1ならin_1の信号を出力します。
  */
class Mux2 extends Module {
  val io = IO(new Bundle {
    val selector = Input(UInt(1.W))
    val in_0 = Input(UInt(1.W))
    val in_1 = Input(UInt(1.W))
    val out = Output(UInt(1.W))
  })

  io.out := (~io.selector & io.in_0) | (io.selector & io.in_1)
}

/** Mux2のコンパニオン・オブジェクト
  */
object Mux2 {
/** マルチプレクサ。
  * @param selector 1ビットの選択信号
  * @param in_0     1ビットの入力信号
  * @param in_1     1ビットの 入力信号
  * 
  * @return selectorが0ならin_0を、1ならin_1の信号を出力します。
  */
  def apply(selector: UInt, in_0: UInt, in_1: UInt): UInt = {
    val mux2 = Module(new Mux2())
    mux2.io.selector := selector
    mux2.io.in_0 := in_0
    mux2.io.in_1 := in_1
    mux2.io.out
  }
}
