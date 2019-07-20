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

/**
  * Mux2のVerilogファイルを生成するための、オブジェクト
  */
object Mux2 extends App {
  chisel3.Driver.execute(args, () => new Mux2())
}
