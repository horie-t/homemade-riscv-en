// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Synchronizer
  */
class Synchronizer extends Module {
  val io = IO(new Bundle {
    val d = Input(Bool())
    val q = Output(Bool())
  })

  val reg1 = RegNext(io.d)
  val reg2 = RegNext(reg1)

  io.q := reg2
}

object Synchronizer {
  def apply(d: Bool): Bool = {
    val synchronizer = Module(new Synchronizer)
    synchronizer.io.d := d
    synchronizer.io.q
  }
}
