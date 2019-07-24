// See LICENSE for license details.

import chisel3._

/**
  * Logic gate (Vector version)
  */
class LogicGatesVec extends Module {
  val io = IO(new Bundle {
    /** Input from switch */
    val switches = Input(Vec(2, Bool()))
    /** Output to LED */
    val leds = Output(Vec(7, Bool()))
  })

  io.leds(0) :=  ~io.switches(0)                    // NOT
  io.leds(1) :=   io.switches(0) & io.switches(1)   // AND
  io.leds(2) :=   io.switches(0) | io.switches(1)   // OR
  io.leds(3) := ~(io.switches(0) & io.switches(1))  // NAND
  io.leds(4) := ~(io.switches(0) | io.switches(1))  // NOR
  io.leds(5) :=   io.switches(0) ^ io.switches(1)   // XOR
  io.leds(6) := ~(io.switches(0) ^ io.switches(1))  // NXOR
}

object LogicGatesVec extends App {
  chisel3.Driver.execute(args, () => new LogicGatesVec())
}
