// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** CharRom
  */
class CharRom extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clka = Input(Clock())
    val ena = Input(Bool())
    val addra = Input(UInt(11.W))
    val douta = Output(UInt(8.W))
  })
}
