// See LICENSE for license details.

import chisel3._

/** Module class that only lights the LED
  */
class LightLED extends Module {
  /** Define input/output port (signal line) */
  val io = IO(new Bundle {
    /** Define ports to go outside of Module */
    val led = Output(Bool())
  })

  // The value of the output port are true
  io.led := true.B 
}

/**
  * Object for generating Verilog file
  */
object LightLED extends App {
  chisel3.Driver.execute(args, () => new LightLED())
}
