// See LICENSE for license details.

import chisel3._

/** 31 bit counter. 7 segment LED displays upper 4 bit value.
  */
class Counter31Bit extends Module {
  val io = IO(new Bundle {
    val seg7led = Output(new Seg7LEDBundle)
  })

  // Register for counter of 31 bit width (Initialize to 0)
  val count = RegInit(0.U(31.W))
  count := count + 1.U // 1 increase per clock

  // display upper 4 bit of counter
  val seg7LED1Digit = Module(new Seg7LED1Digit)
  seg7LED1Digit.io.num := count(30, 27) // display upper 4 bits

  io.seg7led := seg7LED1Digit.io.seg7led
}

object Counter31Bit extends App {
  chisel3.Driver.execute(args, () => new Counter31Bit)
}
