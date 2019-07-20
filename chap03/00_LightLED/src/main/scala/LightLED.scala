// See LICENSE for license details.

import chisel3._

/** LEDを点灯するだけのモジュール・クラス
  */
class LightLED extends Module {
  /** 入出力の信号線を定義 */
  val io = IO(new Bundle {
    /** Moduleの外に出ていく信号線を定義 */
    val led = Output(Bool())
  })

  // 出力信号の内容は、true
  io.led := true.B 
}

/**
  * Verilogファイルを生成するための、オブジェクト
  */
object LightLED extends App {
  chisel3.Driver.execute(args, () => new LightLED())
}
