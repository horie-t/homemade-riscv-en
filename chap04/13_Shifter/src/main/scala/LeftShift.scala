// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Left shifter
  */
class LeftShifter extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(4.W))
    // Since the input is 4 bits, 2 bits are sufficient for
    // specifying the shift amount
    val shiftAmount = Input(UInt(2.W))
    val out = Output(UInt(4.W))
  })

  // The output of the least significant bit becomes the least 
  // significant bit of input when the shift amount is 0, and 
  // becomes 0 otherwise.
  val mux0 = Module(new Mux4)
  mux0.io.selector := io.shiftAmount
  mux0.io.in_0 := io.in(0)
  mux0.io.in_1 := 0.U
  mux0.io.in_2 := 0.U
  mux0.io.in_3 := 0.U

  val mux1 = Module(new Mux4)
  mux1.io.selector := io.shiftAmount
  mux1.io.in_0 := io.in(1)
  mux1.io.in_1 := io.in(0)
  mux1.io.in_2 := 0.U
  mux1.io.in_3 := 0.U

  val mux2 = Module(new Mux4)
  mux2.io.selector := io.shiftAmount
  mux2.io.in_0 := io.in(2)
  mux2.io.in_1 := io.in(1)
  mux2.io.in_2 := io.in(0)
  mux2.io.in_3 := 0.U

  val mux3 = Module(new Mux4)
  mux3.io.selector := io.shiftAmount
  mux3.io.in_0 := io.in(3)
  mux3.io.in_1 := io.in(2)
  mux3.io.in_2 := io.in(1)
  mux3.io.in_3 := io.in(0)

  io.out := Cat(mux3.io.out, mux2.io.out, mux1.io.out, mux0.io.out)
}

/**
  * Object to output Verilog file of Mux4.
  */
object LeftShifter extends App {
  chisel3.Driver.execute(args, () => new LeftShifter)
}
