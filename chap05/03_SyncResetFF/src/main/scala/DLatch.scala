// See LICENSE for license details.

import chisel3._

/** D Latch
  */
class DLatch extends Module {
  val io = IO(new Bundle {
    val data = Input(Bool())
    val enable = Input(Bool())
    val q = Output(Bool())
  })

  val srLatch = Module(new SRLatch)
  srLatch.io.reset := io.enable & ~io.data
  srLatch.io.set   := io.enable &  io.data

  io.q := srLatch.io.q
}

object DLatch extends App {
  chisel3.Driver.execute(args, () => new DLatch)
}
