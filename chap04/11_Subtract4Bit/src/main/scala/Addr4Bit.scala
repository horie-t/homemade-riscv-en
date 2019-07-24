// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Adder (4bit)
  */
class Adder4Bit extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))

    /* By connecting the adders together, it is possible to create a more multi-bit adder. */
    val carryIn = Input(UInt(1.W))

    val sum = Output(UInt(4.W))
    val carryOut = Output(UInt(1.W))
  })

  /*
   * Generate Adders for 4 bits.
   */
  val adder0 = Module(new FullAdder)
  adder0.io.a := io.a(0)
  adder0.io.b := io.b(0)
  adder0.io.carryIn := io.carryIn

  val adder1 = Module(new FullAdder)
  adder1.io.a := io.a(1)
  adder1.io.b := io.b(1)
  adder1.io.carryIn := adder0.io.carryOut // The carry output of the lower digit propagates

  val adder2 = Module(new FullAdder)
  adder2.io.a := io.a(2)
  adder2.io.b := io.b(2)
  adder2.io.carryIn := adder1.io.carryOut

  val adder3 = Module(new FullAdder)
  adder3.io.a := io.a(3)
  adder3.io.b := io.b(3)
  adder3.io.carryIn := adder2.io.carryOut

  // Concatenate the sum of each digit
  io.sum := Cat(adder3.io.sum, adder2.io.sum, adder1.io.sum, adder0.io.sum)

  io.carryOut := adder3.io.carryOut
}

/** Adder (4bit). "for loop" version
  */
class Adder4BitFor extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val carryIn = Input(UInt(1.W))
    val sum = Output(UInt(4.W))
    val carryOut = Output(UInt(1.W))
  })

  val fullAdders = VecInit(Seq.fill(4){ Module(new FullAdder).io }) // [Caution] we pass "io"
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

class AdderLED extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val seg7LEDBundle = Output(new Seg7LEDBundle)
    val overflowLED = Output(UInt(1.W)) // Lights the LED when overflow occurs
  })

  val seg7LED = Module(new Seg7LED1Digit)
  val adder = Module(new Adder4Bit)
  adder.io.a := io.a
  adder.io.b := io.b
  // Since there is no carry input of the least significant digit, 0 is assigned
  adder.io.carryIn := 0.U

  seg7LED.io.num := adder.io.sum

  io.seg7LEDBundle := seg7LED.io.seg7led
  io.overflowLED := adder.io.carryOut
}

class AdderLEDFor extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val seg7LEDBundle = Output(new Seg7LEDBundle)
    val overflowLED = Output(UInt(1.W)) // light LED when overflow occur
  })

  val seg7LED = Module(new Seg7LED1Digit)
  val adder = Module(new Adder4BitFor)
  adder.io.a := io.a
  adder.io.b := io.b
  // Since there is no carry input of the least significant digit, 0 is assigned
  adder.io.carryIn := 0.U

  seg7LED.io.num := adder.io.sum

  io.seg7LEDBundle := seg7LED.io.seg7led
  io.overflowLED := adder.io.carryOut
}

/**
  * Objcect to output Verilog file
  */
object AdderLED extends App {
  chisel3.Driver.execute(args, () => new AdderLED)
}

object AdderLEDFor extends App {
  chisel3.Driver.execute(args, () => new AdderLEDFor)
}
