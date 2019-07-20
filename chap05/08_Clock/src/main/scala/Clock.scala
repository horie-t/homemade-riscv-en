// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** 時計
  */
class Clock extends Module {
  val io = IO(new Bundle {
    val seg7led = Output(new Seg7LEDBundle)
  })

  val (clockNum, oneSec) = Counter(true.B, 100000000) // 100Mクロック分カウント
  val (sec, oneMin)      = Counter(oneSec, 60)        // 60秒カウント
  val (min, oneHour)     = Counter(oneMin, 60)        // 60分カウント
  val (hour, oneDay)     = Counter(oneHour, 24)       // 24時間カウント

  val seg7LED = Module(new Seg7LED)
  seg7LED.io.digits := VecInit(List(sec % 10.U, (sec / 10.U)(3, 0), min % 10.U, (min / 10.U)(3, 0),
    hour % 10.U, (hour / 10.U)(3, 0), 0.U(4.W), 0.U(4.W)))

  io.seg7led := seg7LED.io.seg7led
}

object Clock extends App {
  chisel3.Driver.execute(args, () => new Clock)
}
