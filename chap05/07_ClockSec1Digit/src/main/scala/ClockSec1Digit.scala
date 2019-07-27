// See LICENSE for license details.

import chisel3._
import chisel3.util._

class ClockSec1Digit extends Module {
  val io = IO(new Bundle {
    val seg7led = Output(new Seg7LEDBundle)
  })

  // Counter for 1 second.
  val count1Sec = RegInit(0.U(27.W))

  // Counter from 0 to 15 seconds.
  val count = RegInit(0.U(4.W))

  when (count1Sec === 100000000.U) {
    // Counted up 100 Million times
    count := count + 1.U // increment second.
    count1Sec := 0.U     // reset counter
  } .otherwise {
    // Increment every 1 clock
    count1Sec := count1Sec + 1.U
  }

  val seg7LED1Digit = Module(new Seg7LED1Digit)
  seg7LED1Digit.io.num := count

  io.seg7led := seg7LED1Digit.io.seg7led
}

object ClockSec1Digit extends App {
  chisel3.Driver.execute(args, () => new ClockSec1Digit)
}
