// See LICENSE for license details.

import chisel3._

/** Dフリップフロップ
  */
class DFlipFlop extends Module {
  val io = IO(new Bundle {
    val clock = Input(Bool())
    val data = Input(Bool())
    val q = Output(Bool())
  })

  val dLatch1 = Module(new DLatch)
  dLatch1.io.enable := ~io.clock
  dLatch1.io.data   := io.data

  val dLatch2 = Module(new DLatch)
  dLatch2.io.enable := io.clock
  dLatch2.io.data   := dLatch1.io.q

  io.q := dLatch2.io.q
}

object DFlipFlop extends App {
  chisel3.Driver.execute(args, () => new DFlipFlop)
}
