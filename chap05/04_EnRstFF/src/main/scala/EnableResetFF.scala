// See LICENSE for license details.

import chisel3._

/** リセット可能なイネーブル機能付きフリップフロップ
  */
class EnableResetFF extends Module {
  val io = IO(new Bundle {
    val clock = Input(Bool())
    val reset = Input(Bool())
    val enable = Input(Bool())
    val data = Input(Bool())
    val q = Output(Bool())
  })

  val dFF = Module(new DFlipFlop)
  dFF.io.clock := io.clock
  dFF.io.data  := Mux(io.enable, io.data, dFF.io.q) & ~io.reset

  io.q := dFF.io.q
}

object EnableResetFF extends App {
  chisel3.Driver.execute(args, () => new EnableResetFF)
}
