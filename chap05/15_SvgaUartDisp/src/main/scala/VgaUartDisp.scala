// See LICENSE for license details.

import chisel3._
import chisel3.util._
import chisel3.core.withClock

class VgaUartDisp(dispMode: DispMode) extends Module {
  val io = IO(new Bundle {
    val rxData = Input(Bool())
    val txData = Output(Bool())
    val rts = Output(Bool())
    val cts = Input(Bool())

    val vga = Output(new VgaBundle)
  })

  /* ステート定義 */
  val sIdle :: sCharWrite :: Nil = Enum(2)
  val state = RegInit(sIdle)

  // 文字を表示する、行と列
  val rowChar = RegInit(0.asUInt(5.W))
  val colChar = RegInit(0.asUInt(7.W))

  // 文字の色(単色ではつまらないので、列によって色を変える)
  val colorIndex = (colChar / 10.U)(2, 0)
  val colorChar = Cat(7.U - colorIndex, colorIndex, colorIndex(1, 0))

  val uart = Module(new Uart)
  uart.io.rxData := io.rxData
  io.txData := uart.io.txData
  io.rts := uart.io.rts
  uart.io.cts := io.cts
  uart.io.sendData.valid := false.B // 何もデータは送らない
  uart.io.sendData.bits := 0.U
  uart.io.receiveData.ready := state === sIdle

  val charVramWriter = Module(new CharVramWriter(dispMode))
  charVramWriter.io.charData.bits.row := rowChar
  charVramWriter.io.charData.bits.col := colChar
  charVramWriter.io.charData.bits.charCode := uart.io.receiveData.bits
  charVramWriter.io.charData.bits.color := colorChar
  charVramWriter.io.charData.valid := uart.io.receiveData.valid

  val displayClock = Module(new DisplayClock)
  displayClock.io.clk_system := clock
  withClock (displayClock.io.clk_display) {
    val vgaDisplay = Module(new VgaDisplay(dispMode))
    vgaDisplay.io.vramData <> charVramWriter.io.vramData
    vgaDisplay.io.vramClock := clock

    io.vga := vgaDisplay.io.vga
  }

  // ステート・マシン
  switch (state) {
    is (sIdle) {
      when (uart.io.receiveData.valid) {
        state := sCharWrite
      }
    }
    is (sCharWrite) {
      when (charVramWriter.io.charData.ready) {
        when (colChar === dispMode.col.U - 1.U) {
          when (rowChar === dispMode.row.U - 1.U) {
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
}

object VgaUartDisp extends App {
  chisel3.Driver.execute(args, () => new VgaUartDisp(SVGA))
}
