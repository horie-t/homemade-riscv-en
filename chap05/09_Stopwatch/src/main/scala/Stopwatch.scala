// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Stopwatch
  */
class Stopwatch extends Module {
  val io = IO(new Bundle {
    val startStop = Input(Bool())
    val seg7led = Output(new Seg7LEDBundle)
  })

  val running = RegInit(false.B)
  when (Debounce(io.startStop)) {
    running := ~running
  }

  val (clockNum, centimalSec) = Counter(running, 1000000) // Count for 1M clocks(0.01sec)
  val (cSec, oneSec) = Counter(centimalSec, 100)  // 0.01 second count
  val (sec, oneMin)      = Counter(oneSec, 60)    // 60 seconds count
  val (min, oneHour)     = Counter(oneMin, 60)    // 60 minutes count
  val (hour, oneDay)     = Counter(oneHour, 24)   // 24 hours count

  val seg7LED = Module(new Seg7LED)
  seg7LED.io.digits := VecInit(List(cSec % 10.U, (cSec / 10.U)(3, 0), sec % 10.U, (sec / 10.U)(3, 0),
    min % 10.U, (min / 10.U)(3, 0), hour % 10.U, (hour / 10.U)(3, 0)))

  io.seg7led := seg7LED.io.seg7led
}

object Stopwatch extends App {
  chisel3.Driver.execute(args, () => new Stopwatch)
}
