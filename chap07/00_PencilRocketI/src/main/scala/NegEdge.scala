// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** 入力の立ち下がりを検出します。
  */
class NegEdge extends Module {
  val io = IO(new Bundle {
    val d = Input(Bool())
    val pulse = Output(Bool())
  })

  val reg = RegNext(io.d, false.B)

  io.pulse := reg && !io.d
}

object NegEdge {
  def apply(d: Bool): Bool = {
    val negEdge = Module(new NegEdge)
    negEdge.io.d := d
    negEdge.io.pulse
  }
}
