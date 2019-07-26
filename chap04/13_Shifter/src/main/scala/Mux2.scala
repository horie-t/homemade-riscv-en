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

/** Companion object of Mux2
  */
object Mux2 {
/** Multiplexer
  * @param selector 1-bit select signal
  * @param in_0     1-bit input signal
  * @param in_1     1-bit input signal
  * 
  * @return If selecter is 0, output the value of in_0, otherwise in_1.
  */
  def apply(selector: UInt, in_0: UInt, in_1: UInt): UInt = {
    val mux2 = Module(new Mux2())
    mux2.io.selector := selector
    mux2.io.in_0 := in_0
    mux2.io.in_1 := in_1
    mux2.io.out
  }
}
