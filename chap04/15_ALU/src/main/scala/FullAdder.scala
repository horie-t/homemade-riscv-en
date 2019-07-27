// See LICENSE for license details.

import chisel3._

/** Full Adder
  */
class FullAdder extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(1.W))
    val b = Input(UInt(1.W))
    val carryIn = Input(UInt(1.W))
    val sum = Output(UInt(1.W))
    val carryOut = Output(UInt(1.W))
  })

  // Add up to the same digit
  val halfAddr = Module(new HalfAdder)
  halfAddr.io.a := io.a
  halfAddr.io.b := io.b

  // Addition of same digit sum and carry input
  val halfAddrCarry = Module(new HalfAdder)
  halfAddrCarry.io.a := halfAddr.io.sum
  halfAddrCarry.io.b := io.carryIn

  io.sum := halfAddrCarry.io.sum

  // If a carry output is generated in either of the results of the two half adders, a carry is output
  io.carryOut := halfAddr.io.carryOut | halfAddrCarry.io.carryOut
}

/**
  * Objcect to output Verilog file
  */
object FullAdder extends App {
  chisel3.Driver.execute(args, () => new FullAdder)
}
