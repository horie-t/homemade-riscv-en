// See LICENSE for license details.

import chisel3._

/** 全加算器
  */
class FullAdder extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(1.W))
    val b = Input(UInt(1.W))
    val carryIn = Input(UInt(1.W))
    val sum = Output(UInt(1.W))
    val carryOut = Output(UInt(1.W))
  })

  // まず同一桁の足し算
  val halfAddr = Module(new HalfAdder)
  halfAddr.io.a := io.a
  halfAddr.io.b := io.b

  // 同一桁の合計と桁上げ入力の足し算
  val halfAddrCarry = Module(new HalfAdder)
  halfAddrCarry.io.a := halfAddr.io.sum
  halfAddrCarry.io.b := io.carryIn

  io.sum := halfAddrCarry.io.sum

  // 2つの半加算器の結果のどちらかでも桁上げ出力が発生していたら、全体として桁上げ
  io.carryOut := halfAddr.io.carryOut | halfAddrCarry.io.carryOut
}

/**
  * Verilogファイルを生成するための、オブジェクト
  */
object FullAdder extends App {
  chisel3.Driver.execute(args, () => new FullAdder)
}
