// See LICENSE for license details.

import chisel3._
import chisel3.util._

class ClockSec1Digit extends Module {
  val io = IO(new Bundle {
    val seg7led = Output(new Seg7LEDBundle)
  })

  // 1秒カウンタ
  val count1Sec = RegInit(0.U(27.W))

  // 0から15秒までのカウンタ
  val count = RegInit(0.U(4.W))

  when (count1Sec === 100000000.U) {
    // 100Mクロックカウントしたら
    count := count + 1.U // 秒をインクリメント
    count1Sec := 0.U     // カウンタをリセット
  } .otherwise {
    // 通常は、1クロック毎にインクリメント
    count1Sec := count1Sec + 1.U
  }

  val seg7LED1Digit = Module(new Seg7LED1Digit)
  seg7LED1Digit.io.num := count

  io.seg7led := seg7LED1Digit.io.seg7led
}

object ClockSec1Digit extends App {
  chisel3.Driver.execute(args, () => new ClockSec1Digit)
}
