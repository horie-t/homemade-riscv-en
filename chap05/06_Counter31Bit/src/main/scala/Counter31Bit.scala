// See LICENSE for license details.

import chisel3._

/** 31ビットカウンタ。上位4ビットの値を7セグメントLEDで表示。
  */
class Counter31Bit extends Module {
  val io = IO(new Bundle {
    val seg7led = Output(new Seg7LEDBundle)
  })

  // 31ビット幅のカウント用のレジスタ(0で初期化する)
  val count = RegInit(0.U(31.W))
  count := count + 1.U // 1クロック毎に1増加

  // カウントの一部のビットの値を表示
  val seg7LED1Digit = Module(new Seg7LED1Digit)
  seg7LED1Digit.io.num := count(30, 27) // 上位4ビットでの値を表示させる

  io.seg7led := seg7LED1Digit.io.seg7led
}

object Counter31Bit extends App {
  chisel3.Driver.execute(args, () => new Counter31Bit)
}
