// See LICENSE for license details.

import chisel3._
import chisel3.util._

class VgaUartDisp extends Module {
  val io = IO(new Bundle {
    val rxData = Input(Bool())
    val txData = Output(Bool())
    val rts = Output(Bool())
    val cts = Input(Bool())

    val vga = Output(new VgaBundle)
  })

  /* State machine definition */
  val sIdle :: sCharWrite :: Nil = Enum(2)
  val state = RegInit(sIdle)

  // Row and column to display character
  val rowChar = RegInit(0.U(5.W))
  val colChar = RegInit(0.U(7.W))

  // Character color
  // (Because it is boring in a single color, change the color by the row)
  val colorIndex = (colChar / 10.U)(2, 0)
  val colorChar = Cat(7.U - colorIndex, colorIndex, colorIndex(1, 0))

  val uart = Module(new Uart)
  uart.io.rxData := io.rxData
  io.txData := uart.io.txData
  io.rts := uart.io.rts
  uart.io.cts := io.cts
  uart.io.sendData.valid := false.B // No data is sent
  uart.io.sendData.bits := 0.U
  uart.io.receiveData.ready := state === sIdle

  val charVramWriter = Module(new CharVramWriter)
  charVramWriter.io.charData.bits.row := rowChar
  charVramWriter.io.charData.bits.col := colChar
  charVramWriter.io.charData.bits.charCode := uart.io.receiveData.bits
  charVramWriter.io.charData.bits.color := colorChar
  charVramWriter.io.charData.valid := uart.io.receiveData.valid

  val vgaDisplay = Module(new VgaDisplay)
  vgaDisplay.io.vramData <> charVramWriter.io.vramData

  // State transition
  switch (state) {
    is (sIdle) {
      when (uart.io.receiveData.valid) {
        state := sCharWrite
      }
    }
    is (sCharWrite) {
      when (charVramWriter.io.charData.ready) {
        when (colChar === 79.U) {
          when (rowChar === 29.U) {
            rowChar := 0.U
          } .otherwise {
            rowChar := rowChar + 1.U
          }
          colChar := 0.U
        } .otherwise {
          colChar := colChar + 1.U
        }
        state := sIdle
      }
    }
  }

  io.vga := vgaDisplay.io.vga
}

object VgaUartDisp extends App {
  chisel3.Driver.execute(args, () => new VgaUartDisp)
}
