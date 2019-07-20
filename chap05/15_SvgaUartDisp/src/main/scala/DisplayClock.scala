// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** DisplayClock
  */
class DisplayClock extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk_system = Input(Clock())
    val clk_display = Output(Clock())
  })
}
