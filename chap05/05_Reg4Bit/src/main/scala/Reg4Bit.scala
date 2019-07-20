// See LICENSE for license details.

import chisel3._
import chisel3.core.withReset
import chisel3.util._

/** 4ビットレジスタ
  */
class Reg4Bit extends Module {
  val io = IO(new Bundle {
    val resetN = Input(Bool())
    val in = Input(UInt(4.W))
    val enable = Input(Bool())
    val out = Output(UInt(4.W))
  })

  withReset (~io.resetN) {
    io.out := RegEnable(io.in, 0.U(4.W), io.enable)
  }
}

object Reg4Bit extends App {
  chisel3.Driver.execute(args, () => new Reg4Bit)
}
