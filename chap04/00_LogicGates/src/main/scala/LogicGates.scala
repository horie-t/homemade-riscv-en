// See LICENSE for license details.

import chisel3._

/**
  * Boolean operations
  */
class LogicGates extends Module {
  val io = IO(new Bundle {
    val switch_a = Input(Bool()) // input A
    val switch_b = Input(Bool()) // input B
    val led_not = Output(Bool()) // connect result of "NOT A" to LED
    val led_and = Output(Bool()) // AND
    val led_or = Output(Bool())  // OR
    val led_nand = Output(Bool()) // NAND
    val led_nor = Output(Bool())  // NOR
    val led_xor = Output(Bool())  // XOR
    val led_nxor = Output(Bool()) // NXOR
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
