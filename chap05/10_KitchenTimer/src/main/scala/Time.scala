// See LICENSE for license details.

import chisel3._
import chisel3.util._

object Time {
  // Bit width required for 60-base notation (0 to 59)
  val sexagesimalWitdh = (log2Floor(59) + 1).W
}

/** Model class of time
  */
class Time extends Module {
  val io = IO(new Bundle {
    val incMin = Input(Bool()) // Increase time by one minute.
    val incSec = Input(Bool()) // Increase time by one second.
    val countDown = Input(Bool()) // Run a countdown.

    val min = Output(UInt(Time.sexagesimalWitdh)) // Remaining minutes.
    val sec = Output(UInt(Time.sexagesimalWitdh)) // Remaining seconds.
    val zero = Output(Bool())                     // Whether the remaining time is gone
  })

  val min = RegInit(0.U(Time.sexagesimalWitdh))
  when (io.incMin) {
    when (min === 59.U) {
      min := 0.U
    } .otherwise {
      min := min + 1.U
    }
  }

  val sec = RegInit(0.U(Time.sexagesimalWitdh))
  when (io.incSec) {
    when (sec === 59.U) {
      sec := 0.U
    } .otherwise {
      sec := sec + 1.U
    }
  }

  val zero = Wire(Bool())
  // Counter for less than 1 second
  val (count, oneSec) = Counter(io.countDown && !zero, 100000000)
  zero := min === 0.U && sec === 0.U && count === 0.U

  when (io.countDown && oneSec) {
    when (sec === 0.U) {
      when (min =/= 0.U) {
        min := min - 1.U
        sec := 59.U
      }
    } .otherwise {
      sec := sec - 1.U
    }
  }

  io.min := min
  io.sec := sec
  io.zero := zero
}
