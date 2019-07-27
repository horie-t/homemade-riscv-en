// See LICENSE for license details.

import chisel3._

/** Synchronous resetable flip flop
  */
class SyncResetFF extends Module {
  val io = IO(new Bundle {
    val clock = Input(Bool())
    val reset = Input(Bool())
    val data = Input(Bool())
    val q = Output(Bool())
  })

  val dFF = Module(new DFlipFlop)
  dFF.io.clock := io.clock
  dFF.io.data  := io.data & ~io.reset

  io.q := dFF.io.q
}

class SyncRFFWrapper extends Module {
  val io = IO(new Bundle {
    val clock = Input(Bool())
    val resetNegate = Input(Bool())
    val data = Input(Bool())
    val q = Output(Bool())
  })

  val srFF = Module(new SyncResetFF)
  srFF.io.clock := io.clock
  srFF.io.reset := ~io.resetNegate
  srFF.io.data  := io.data

  io.q := srFF.io.q
}

object SyncRFFWrapper extends App {
  chisel3.Driver.execute(args, () => new SyncRFFWrapper)
}
