// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** 乗算器(4bit)
  */
class Multiplier4Bit extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val result = Output(UInt(8.W))
  })

  // 各桁の計算をする
  val digit0 = Wire(UInt(4.W))
  val digit1 = Wire(UInt(4.W))
  val digit2 = Wire(UInt(4.W))
  val digit3 = Wire(UInt(4.W))
  digit0 := io.a & Fill(4, io.b(0)) // b(0)を4つコピーしてからANDをとる
  digit1 := io.a & Fill(4, io.b(1))
  digit2 := io.a & Fill(4, io.b(2))
  digit3 := io.a & Fill(4, io.b(3))

  // 0桁目と1桁目の足し算
  val adder1 = Module(new Adder4Bit)
  adder1.io.a := digit1
  adder1.io.b := Cat(0.U, digit0(3, 1)) // digit0の3〜1ビット目を使っている
  adder1.io.carryIn := 0.U              // 桁上げ入力は不要

  // 1桁目の足し算の結果と2桁目の足し算
  val adder2 = Module(new Adder4Bit)
  adder2.io.a := digit2
  adder2.io.b := Cat(adder1.io.carryOut, adder1.io.sum(3, 1))
  adder2.io.carryIn := 0.U

  val adder3 = Module(new Adder4Bit)
  adder3.io.a := digit3
  adder3.io.b := Cat(adder2.io.carryOut, adder2.io.sum(3, 1))
  adder3.io.carryIn := 0.U

  io.result := Cat(adder3.io.carryOut, adder3.io.sum,
    adder2.io.sum(0), adder1.io.sum(0), digit0(0))
}

/** 乗算器(4bit)
  */
class Multiplier4Bit2 extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val result = Output(UInt(8.W))
  })

  val digits = VecInit(
    for (i <- 0 until 4) yield { io.a & Fill(4, io.b(i)) }
  )

  val adders = VecInit(Seq.fill(3){ Module(new Adder4Bit).io })
  for (i <- 0 until 3) {
    adders(i).a := digits(i + 1)
    if (i == 0) {
      adders(i).b := Cat(0.U, digits(i)(3, 1)) // digits(0)(3, 1)はdigits(0).apply(3, 1)の略
    } else {
      adders(i).b := Cat(adders(i - 1).carryOut, adders(i - 1).sum(3, 1))
    }
    adders(i).carryIn := 0.U              // 桁上げ入力は不要
  }

  io.result := Cat(adders(2).carryOut, adders(2).sum,
    adders(1).sum(0), adders(0).sum(0), digits(0)(0))
}

/** 乗算器
  * 
  * @param n ビット幅
  */
class Multiplier(n: Int) extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(n.W))
    val b = Input(UInt(n.W))
    val result = Output(UInt((2 * n).W))
  })

  val digits = VecInit(
    for (i <- 0 until n) yield { io.a & Fill(n,  io.b(i)) }
  )

  val adders = VecInit(Seq.fill(n - 1){ Module(new Adder4Bit).io })
  for (i <- 0 until (n - 1)) {
    adders(i).a := digits(i + 1)
    if (i == 0) {
      // digits(0)(n - 1, 1)はdigits(0).apply(n - 1, 1)の略
      adders(i).b := Cat(0.U, digits(i)(n - 1, 1)) 
    } else {
      adders(i).b := Cat(adders(i - 1).carryOut, adders(i - 1).sum(n - 1, 1))
    }
    adders(i).carryIn := 0.U              // 桁上げ入力は不要
  }

  val adderLsbs = Cat(for (i <- (n - 2) to 0 by -1) yield { adders(i).sum(0) })
  io.result := Cat(adders(n - 2).carryOut, adders(n - 2).sum(n - 1, 1), adderLsbs, digits(0)(0))
}

object Multiplier4Bit extends App {
  chisel3.Driver.execute(args, () => new Multiplier4Bit)
}

object Multiplier4Bit2 extends App {
  chisel3.Driver.execute(args, () => new Multiplier4Bit2)
}

object Multiplier extends App {
  chisel3.Driver.execute(args, () => new Multiplier(4))
}
