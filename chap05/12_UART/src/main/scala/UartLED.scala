// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** Sample module of UART
  */
class UartLED extends Module {
  val io = IO(new Bundle {
    val rxData = Input(Bool())
    val txData = Output(Bool())
    val rts = Output(Bool())
    val cts = Input(Bool())
    val sendValue = Input(UInt(8.W))
    val valid = Input(Bool())
    val seg7led = Output(new Seg7LEDBundle)
  })

  val uart = Module(new Uart)
  uart.io.rxData := io.rxData
  io.txData := uart.io.txData

  uart.io.sendData.bits := io.sendValue
  uart.io.sendData.valid := Debounce(io.valid)

  uart.io.receiveData.ready := uart.io.receiveData.valid

  io.rts := uart.io.rts
  uart.io.cts := io.cts

  // output
  val seg7LED = Module(new Seg7LED)
  seg7LED.io.digits := VecInit(List(uart.io.receiveData.bits(3, 0), uart.io.receiveData.bits(7, 4))
    ::: List.fill(6) { 0.U(4.W) })
  seg7LED.io.blink := false.B
  io.seg7led := seg7LED.io.seg7led
}

object UartLED extends App {
  chisel3.Driver.execute(args, () => new UartLED)
}
