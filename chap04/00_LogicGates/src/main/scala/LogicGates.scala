// See LICENSE for license details.

import chisel3._

/**
  * 各種論理演算
  */
class LogicGates extends Module {
  val io = IO(new Bundle {
    val switch_a = Input(Bool()) // 入力A
    val switch_b = Input(Bool()) // 入力B
    val led_not = Output(Bool()) // NOT Aの結果をLEDに繋げる
    val led_and = Output(Bool()) // ANDの〃
    val led_or = Output(Bool())  // ORの〃
    val led_nand = Output(Bool()) // NANDの〃
    val led_nor = Output(Bool())  // NORの〃
    val led_xor = Output(Bool())  // XORの〃
    val led_nxor = Output(Bool()) // NXORの
  })

  io.led_not  := ~io.switch_a
  io.led_and  := io.switch_a & io.switch_b
  io.led_or   := io.switch_a | io.switch_b
  io.led_nand := ~(io.switch_a & io.switch_b)
  io.led_nor  := ~(io.switch_a | io.switch_b)
  io.led_xor  := io.switch_a ^ io.switch_b
  io.led_nxor := ~(io.switch_a ^ io.switch_b)
}

object LogicGates extends App {
  chisel3.Driver.execute(args, () => new LogicGates())
}
