// See LICENSE for license details.

import chisel3._

/** Multiplexer.
  * If selecter is 0, output the value of in_0, otherwise in_1.
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
  * Object to output Verilog file of Mux2.
  */
object Mux2 extends App {
  chisel3.Driver.execute(args, () => new Mux2())
}
