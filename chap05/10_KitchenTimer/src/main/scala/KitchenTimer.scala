// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Kitchen timer
  */
class KitchenTimer extends Module {
  val io = IO(new Bundle {
    val min = Input(Bool())
    val sec = Input(Bool())
    val startStop = Input(Bool())
    val seg7led = Output(new Seg7LEDBundle)
  })

  /* State machine definition */
  //  "Time setting"  :: "Counting down" :: "Pause" :: "Countdown complete"
  val sTimeSet :: sRun :: sPause :: sFin :: Nil = Enum(4)
  val state = RegInit(sTimeSet)
  // Pressing the start / stop button is a state transition event.
  val stateChange = Debounce(io.startStop)

  // Time management
  val time = Module(new Time)
  // You can change the time only in the "Time setting" state or "Pause" state
  time.io.incMin := (state === sTimeSet || state === sPause) && Debounce(io.min)
  time.io.incSec := (state === sTimeSet || state === sPause) && Debounce(io.sec)
  time.io.countDown := state === sRun

  // State machine transition processing
  when (stateChange) {
    switch (state) {
      is (sTimeSet) {
        when (!time.io.zero) {
          state := sRun
        }
      }
      is (sRun) {
        state := sPause
      }
      is (sPause) {
        state := sRun
      }
      is (sFin)   {
        state := sTimeSet
      }
    }
  } .elsewhen (state === sRun && time.io.zero) {
    state := sFin
  }

  // output
  val seg7LED = Module(new Seg7LED)
  seg7LED.io.digits := VecInit(List(time.io.sec % 10.U, (time.io.sec / 10.U)(3, 0),
    time.io.min % 10.U, (time.io.min / 10.U)(3, 0)) ::: List.fill(4) { 0.U(4.W) })
  seg7LED.io.blink := (state === sFin)
  io.seg7led := seg7LED.io.seg7led
}

object KitchenTimer extends App {
  chisel3.Driver.execute(args, () => new KitchenTimer)
}
