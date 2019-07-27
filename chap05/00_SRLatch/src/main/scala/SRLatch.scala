// See LICENSE for license details.

import chisel3._

/** SR Latch
  */
class SRLatch extends Module {
  val io = IO(new Bundle {
    val set = Input(Bool())
    val reset = Input(Bool())
    val q = Output(Bool())
    val notQ = Output(Bool())
  })

  io.q    := ~(io.reset | io.notQ)
  io.notQ := ~(io.set   | io.q)
}

object SRLatch extends App {
  chisel3.Driver.execute(args, () => new SRLatch)
}
