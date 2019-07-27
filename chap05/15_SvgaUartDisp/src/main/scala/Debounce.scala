// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Debounce module for push button
  */
class Debounce extends Module {
  val io = IO(new Bundle{
    val in = Input(Bool())
    val out = Output(Bool())
  })

  // Capture values at 100 millisecond intervals
  val (count, enable) = Counter(true.B, 10000000) 

  val reg0 = RegEnable(io.in, false.B, enable)
  val reg1 = RegEnable(reg0,  false.B, enable)

  // Change is detected only when enable, and it makes pulse of 1 clock
  io.out := reg0 && !reg1 && enable 
}

/** Debounce module for push button(Singleton object)
  */
object Debounce {
  def apply(in: Bool): Bool = {
    val debounce = Module(new Debounce)
    debounce.io.in := in
    debounce.io.out
  }
}
