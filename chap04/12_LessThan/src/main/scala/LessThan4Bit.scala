// See LICENSE for license details.

import chisel3._
import chisel3.util._

/**
  * 4 bit less than
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

  // When the signs are the same, it is determined by the sign of the result.
  // If it is different, it is determined by the sign of a.
  // (positive - negative = positive. negative - positive = negative)
  io.result := Mux(io.a(3) === io.b(3), adder.io.sum(3), io.a(3))
}

object LessThan4Bit extends App {
  chisel3.Driver.execute(args, () => new LessThan4Bit)
}
