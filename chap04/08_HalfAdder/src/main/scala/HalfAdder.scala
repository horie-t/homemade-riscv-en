// See LICENSE for license details.

import chisel3._

/** Half Adder
  */
class HalfAdder extends Module {
  /** Define input / output port */
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
  * Objcect to output Verilog file
  */
object HalfAdder extends App {
  chisel3.Driver.execute(args, () => new HalfAdder)
}
