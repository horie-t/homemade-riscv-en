// See LICENSE for license details.

import chisel3._

/**
  * 各種論理演算(ベクトル版)
  */
class LogicGatesVec extends Module {
  val io = IO(new Bundle {
    /** スイッチの入力 */
    val switches = Input(Vec(2, Bool()))
    /** LEDへの出力 */
    val leds = Output(Vec(7, Bool()))
  })

  io.leds(0) :=  ~io.switches(0)                    // NOT
  io.leds(1) :=   io.switches(0) & io.switches(1)   // AND
  io.leds(2) :=   io.switches(0) | io.switches(1)   // OR
  io.leds(3) := ~(io.switches(0) & io.switches(1))  // NAND
  io.leds(4) := ~(io.switches(0) | io.switches(1))  // NOR
  io.leds(5) :=   io.switches(0) ^ io.switches(1)   // XOR
  io.leds(6) := ~(io.switches(0) ^ io.switches(1))  // NXOR
}

object LogicGatesVec extends App {
  chisel3.Driver.execute(args, () => new LogicGatesVec())
}
