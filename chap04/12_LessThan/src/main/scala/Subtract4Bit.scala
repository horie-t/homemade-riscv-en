// See LICENSE for license details.

import chisel3._

class Subtract4Bit extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val result = Output(UInt(4.W))
  })

  val adder = Module(new Adder4Bit)
  adder.io.a := io.a
  adder.io.b := ~(io.b)
  adder.io.carryIn := 1.U

  io.result := adder.io.sum
}

object Subtract4Bit extends App {
  chisel3.Driver.execute(args, () => new Subtract4Bit)
}
