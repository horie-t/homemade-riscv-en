// See LICENSE for license details.

import chisel3._

/** いつまでたっても、結果が定まらないAdder
  */
class UnStableAdder extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val sum = Output(UInt(4.W))
  })

  val adder4Bit = Module(new Adder4Bit)
  adder.io.a := io.a
  adder.io.b := adder.io.sum
  io.sum := adder.sum
}
