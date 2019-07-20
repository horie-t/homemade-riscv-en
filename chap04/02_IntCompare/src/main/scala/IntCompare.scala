// See LICENSE for license details.

import chisel3._

/**
  * 比較演算
  */
class IntCompare extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W)) // 入力A
    val b = Input(UInt(4.W)) // 入力B
    val bit_ope = Output(Bool())
    val bit_reduction = Output(Bool())
    val equal_ope = Output(Bool())
    val equal_5 = Output(Bool())
    val not_5 = Output(Bool())
  })

  // ビット毎に比較して、1つでも違いあれば等しくない。
  io.bit_ope := ~(io.a(0) ^ io.b(0) | io.a(1) ^ io.b(1) | io.a(2) ^ io.b(2) | io.a(3) ^ io.b(3))

  // aとbのビット毎の比較は、a ^ bのようにまとめて出来る。
  // a.orRのようにすると、a(0) | a(1) | a(2) | a(3)と同じになる。
  io.bit_reduction := ~((io.a ^ io.b).orR)

  // Chiselにも、等値演算子があるので、上記のような面倒な事は実際はしない。
  io.equal_ope := io.a === io.b

  // 数値リテラルとの比較もできる。不足するビットは、大きい方に合わせる。
  io.equal_5 := io.a === 5.U

  // 等しくない場合は、=/=。
  io.not_5 := io.a =/= 5.U
}

object IntCompare extends App {
  chisel3.Driver.execute(args, () => new IntCompare())
}
