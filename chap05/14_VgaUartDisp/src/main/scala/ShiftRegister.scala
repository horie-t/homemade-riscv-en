// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Shift register(Serial-In, Parallel-Out)
  * 
  * @param n Bit width of output
  */
class ShiftRegisterSIPO(n: Int) extends Module {
  val io = IO(new Bundle {
    val shiftIn = Input(Bool()) // Input signal
    val enable = Input(Bool())  // When is true, read shiftIn signal.
    val q = Output(UInt(n.W))   // output. The old input is the upper bit.
  })

  val reg = RegInit(0.U(n.W))
  when (io.enable) {
    reg := Cat(reg(n - 2, 0), io.shiftIn)
  }

  io.q := reg
}

/** Shift register(Parallel-In, Serial-Out)
  * 
  * @param n Bit width of output
  */
class ShiftRegisterPISO(n: Int) extends Module {
  val io = IO(new Bundle {
    val d = Input(UInt(n.W))      // Input signal
    val load = Input(Bool())      // when is true, read d
    val enable = Input(Bool())    // when is true, shift
    val shiftOut = Output(Bool()) // output. output from upper bit.
  })

  val reg = RegInit(0.U(n.W))
  when (io.load) {
    reg := io.d
  } .elsewhen (io.enable) {
    reg := Cat(reg(n - 2, 0), true.B)
  }

  io.shiftOut := reg(n - 1)
}
