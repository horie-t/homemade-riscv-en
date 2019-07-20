// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** シフトレジスタ。直列入力-並列出力形(Serial-In, Parallel-Out)
  * 
  * @param n 出力ビット幅
  */
class ShiftRegisterSIPO(n: Int) extends Module {
  val io = IO(new Bundle {
    val shiftIn = Input(Bool()) // 入力。
    val enable = Input(Bool())  // trueの時にshiftInを取り込みます。
    val q = Output(UInt(n.W))   // 出力。古い入力が上位ビットになります。
  })

  val reg = RegInit(0.U(n.W))
  when (io.enable) {
    reg := Cat(reg(n - 2, 0), io.shiftIn)
  }

  io.q := reg
}

/** シフトレジスタ。並列入力-直列出力形(Parallel-In, Serial-Out)
  * 
  * @param n       入力ビット幅
  */
class ShiftRegisterPISO(n: Int) extends Module {
  val io = IO(new Bundle {
    val d = Input(UInt(n.W))      // 入力。
    val load = Input(Bool())      // trueの時にdを読み込みます
    val enable = Input(Bool())    // trueの時にシフトします。
    val shiftOut = Output(Bool()) // 出力。上位ビットから出力します。
  })

  val reg = RegInit(0.U(n.W))
  when (io.load) {
    reg := io.d
  } .elsewhen (io.enable) {
    reg := Cat(reg(n - 2, 0), true.B)
  }

  io.shiftOut := reg(n - 1)
}
