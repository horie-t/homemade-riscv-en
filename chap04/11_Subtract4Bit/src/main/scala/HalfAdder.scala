// See LICENSE for license details.

import chisel3._

/** 半加算器
  */
class HalfAdder extends Module {
  /** 入出力の信号線を定義 */
  val io = IO(new Bundle {
    val a = Input(UInt(1.W))
    val b = Input(UInt(1.W))
    val sum = Output(UInt(1.W))
    val carryOut = Output(UInt(1.W))
  })

  io.sum      := io.a ^ io.b
  io.carryOut := io.a & io.b
}

/**
  * Verilogファイルを生成するための、オブジェクト
  */
object HalfAdder extends App {
  chisel3.Driver.execute(args, () => new HalfAdder)
}
