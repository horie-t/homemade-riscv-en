// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** 加算器(4bit)
  */
class Adder4Bit extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))

    /* この加算器同士を繋いで、更に多ビットの加算器を作れるようにする。 */
    val carryIn = Input(UInt(1.W))

    val sum = Output(UInt(4.W))
    val carryOut = Output(UInt(1.W))
  })

  /*
   * 4ビット分のAdderを生成する。
   */
  val adder0 = Module(new FullAdder)
  adder0.io.a := io.a(0)
  adder0.io.b := io.b(0)
  adder0.io.carryIn := io.carryIn

  val adder1 = Module(new FullAdder)
  adder1.io.a := io.a(1)
  adder1.io.b := io.b(1)
  adder1.io.carryIn := adder0.io.carryOut // 下位の桁の桁上げ出力が伝搬してくる

  val adder2 = Module(new FullAdder)
  adder2.io.a := io.a(2)
  adder2.io.b := io.b(2)
  adder2.io.carryIn := adder1.io.carryOut

  val adder3 = Module(new FullAdder)
  adder3.io.a := io.a(3)
  adder3.io.b := io.b(3)
  adder3.io.carryIn := adder2.io.carryOut

  // 各桁の合計を連結する
  io.sum := Cat(adder3.io.sum, adder2.io.sum, adder1.io.sum, adder0.io.sum)

  io.carryOut := adder3.io.carryOut
}

/** 加算器(4bit)。forループ版
  */
class Adder4BitFor extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val carryIn = Input(UInt(1.W))
    val sum = Output(UInt(4.W))
    val carryOut = Output(UInt(1.W))
  })

  val fullAdders = VecInit(Seq.fill(4){ Module(new FullAdder).io }) // [注意]ioを渡している
  val carries = Wire(Vec(5, UInt(1.W)))
  val sum = Wire(Vec(4, UInt(1.W)))

  carries(0) := io.carryIn

  for (i <- 0 until 4) {
    fullAdders(i).a := io.a(i)
    fullAdders(i).b := io.b(i)
    fullAdders(i).carryIn := carries(i)
    sum(i) := fullAdders(i).sum
    carries(i + 1) := fullAdders(i).carryOut
  }

  io.sum := sum.asUInt
  io.carryOut := carries(4)
}

/** 加算器。
  * 
  * @param n ビット幅
  */
class Adder(n: Int) extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(n.W))
    val b = Input(UInt(n.W))
    val carryIn = Input(UInt(1.W))
    val sum = Output(UInt(n.W))
    val carryOut = Output(UInt(1.W))
  })

  /** デフォルトのビット幅のコンストラクタ
    */
  def this() = this(4)

  val fullAdders = VecInit(Seq.fill(n){ Module(new FullAdder).io })
  val carries = Wire(Vec(n + 1, UInt(1.W)))
  val sum = Wire(Vec(n, UInt(1.W)))

  carries(0) := io.carryIn

  for (i <- 0 until n) {
    fullAdders(i).a := io.a(i)
    fullAdders(i).b := io.b(i)
    fullAdders(i).carryIn := carries(i)
    sum(i) := fullAdders(i).sum
    carries(i + 1) := fullAdders(i).carryOut
  }

  io.sum := sum.asUInt
  io.carryOut := carries(n)
}

class AdderLED extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val seg7LEDBundle = Output(new Seg7LEDBundle)
    val overflowLED = Output(UInt(1.W)) // 桁溢れが発生したらLEDを点灯させる
  })

  val seg7LED = Module(new Seg7LED1Digit)
  val adder = Module(new Adder4Bit)
  adder.io.a := io.a
  adder.io.b := io.b
  adder.io.carryIn := 0.U // 最下位桁の桁上げ入力は存在しないので0を割り当て

  seg7LED.io.num := adder.io.sum

  io.seg7LEDBundle := seg7LED.io.seg7led
  io.overflowLED := adder.io.carryOut
}

class AdderLEDFor extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val seg7LEDBundle = Output(new Seg7LEDBundle)
    val overflowLED = Output(UInt(1.W)) // 桁溢れが発生したらLEDを点灯させる
  })

  val seg7LED = Module(new Seg7LED1Digit)
  val adder = Module(new Adder4BitFor)
  adder.io.a := io.a
  adder.io.b := io.b
  adder.io.carryIn := 0.U // 最下位桁の桁上げ入力は存在しないので0を割り当て

  seg7LED.io.num := adder.io.sum

  io.seg7LEDBundle := seg7LED.io.seg7led
  io.overflowLED := adder.io.carryOut
}

/**
  * Verilogファイルを生成するための、オブジェクト
  */
object AdderLED extends App {
  chisel3.Driver.execute(args, () => new AdderLED)
}

object AdderLEDFor extends App {
  chisel3.Driver.execute(args, () => new AdderLEDFor)
}

object Adder extends App {
  // 8ビットの加算器を生成
  chisel3.Driver.execute(args, () => new Adder(8))
}

