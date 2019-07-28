// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Bundle for 7-segment LED
  */
class Seg7LEDBundle extends Bundle {
  /** For glowing each segment. Make 0 to 7 correspond to CA to CG. It lights when it is 0, and goes out when it is 1. */
  val cathodes = Output(UInt(7.W))
  /** For decimal point. It lights when it is 0, and goes out when it is 1.  */
  val decimalPoint = Output(UInt(1.W))
  /** For digit selection. The digit which is 1 glows up. */
  val anodes = Output(UInt(8.W))
}

/** 7 segment LED (8 digit)
  */
class Seg7LED extends Module {
  val io = IO(new Bundle {
    // allocate 8 digit 4 bit values.
    val digits = Input(Vec(8, UInt(4.W))) 
    val seg7led = Output(new Seg7LEDBundle)
  })

  /* Counter of time to switch each digit.
   * First argument is condition to count up.
   * Second argument is the number counting up. (n, count from 0 to n-1)
   * Return value is taple of number of counter and 
   * signal that becomes ture.B when counter equals to n - 1.
   * If condition is true.B, count every clock. */
  val (digitChangeCount, digitChange) = Counter(true.B, 100000) 

  val (digitIndex, digitWrap) = Counter(digitChange, 8) // Digit to display
  val digitNum = io.digits(digitIndex)        // number of display digit

  io.seg7led.cathodes := MuxCase("b111_1111".U,
    Array(                   // gfe_dcba is order of cathodes
      (digitNum === "h0".U) -> "b100_0000".U,
      (digitNum === "h1".U) -> "b111_1001".U,
      (digitNum === "h2".U) -> "b010_0100".U,
      (digitNum === "h3".U) -> "b011_0000".U,
      (digitNum === "h4".U) -> "b001_1001".U,
      (digitNum === "h5".U) -> "b001_0010".U,
      (digitNum === "h6".U) -> "b000_0010".U,
      (digitNum === "h7".U) -> "b101_1000".U,
      (digitNum === "h8".U) -> "b000_0000".U,
      (digitNum === "h9".U) -> "b001_0000".U,
      (digitNum === "ha".U) -> "b000_1000".U,
      (digitNum === "hb".U) -> "b000_0011".U,
      (digitNum === "hc".U) -> "b100_0110".U,
      (digitNum === "hd".U) -> "b010_0001".U,
      (digitNum === "he".U) -> "b000_0110".U,
      (digitNum === "hf".U) -> "b000_1110".U))

  val anodes = RegInit("b1111_1110".U(8.W))
  when (digitChange) {
    // Rotate shift when switching display digit
    anodes := Cat(anodes(6, 0), anodes(7))
  }
  io.seg7led.anodes := anodes

  io.seg7led.decimalPoint := 1.U         // Do not light the decimal point.
}

/**
  * 7 segment LED lighting module(1 digit version)
  */
class Seg7LED1Digit extends Module {
  val io = IO(new Bundle {
    val num = Input(UInt(4.W))
    val seg7led = Output(new Seg7LEDBundle) // Since it is a constructor that takes no arguments, parentheses after the type name are omitted
  })

  // define alias
  private val seg7led = io.seg7led
  private val num = io.num

  // Default value (all turned off). It is not used because all 16 patterns are defined.
  seg7led.cathodes := MuxCase("b111_1111".U, 
    Array(               // gfe_dcba is order of cathodes
      (num === "h0".U) -> "b100_0000".U,
      (num === "h1".U) -> "b111_1001".U,
      (num === "h2".U) -> "b010_0100".U,
      (num === "h3".U) -> "b011_0000".U,
      (num === "h4".U) -> "b001_1001".U,
      (num === "h5".U) -> "b001_0010".U,
      (num === "h6".U) -> "b000_0010".U,
      (num === "h7".U) -> "b101_1000".U,
      (num === "h8".U) -> "b000_0000".U,
      (num === "h9".U) -> "b001_0000".U,
      (num === "ha".U) -> "b000_1000".U,
      (num === "hb".U) -> "b000_0011".U,
      (num === "hc".U) -> "b100_0110".U,
      (num === "hd".U) -> "b010_0001".U,
      (num === "he".U) -> "b000_0110".U,
      (num === "hf".U) -> "b000_1110".U))

  seg7led.decimalPoint := 1.U         // Do not light the decimal point.
  seg7led.anodes := "b1111_1110".U    // Only the 0th digit light
}

object Seg7LED1Digit extends App {
  chisel3.Driver.execute(args, () => new Seg7LED1Digit)
}
