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

