// See LICENSE for license details.

import chisel3._
import chisel3.util._

/**
  * 小なりの関係演算
  */
class LessThan4Bit extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val result = Output(Bool())
  })

  val adder = Module(new Adder4Bit)
  adder.io.a := io.a
  adder.io.b := ~(io.b)
  adder.io.carryIn := 1.U

  // 符号が同じ場合は結果の符号を見ればよい。
  // 違う場合は、aの方の符号を見ればよい。(正 - 負 = 正、負 - 正 = 負 なので)
  io.result := Mux(io.a(3) === io.b(3), adder.io.sum(3), io.a(3))
}

object LessThan4Bit extends App {
  chisel3.Driver.execute(args, () => new LessThan4Bit)
}
