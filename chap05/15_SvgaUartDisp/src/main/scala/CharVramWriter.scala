// See LICENSE for license details.

import chisel3._
import chisel3.util._

class CharDispBundle extends Bundle {
  val row = Input(UInt(5.W))
  val col = Input(UInt(7.W))
  val charCode = Input(UInt(8.W))
  val color = Input(UInt(8.W))
}

class CharVramWriter(mode: DispMode) extends Module {
  val io = IO(new Bundle{
    val charData = Flipped(Decoupled(new CharDispBundle))
    val vramData = Decoupled(new VramDataBundle)
  })

  /* ステート定義 */
  val sIdle :: sRomRead :: sShiftRegLoad :: sVramWrite :: Nil = Enum(4)
  val state = RegInit(sIdle)

  // 処理中の文字の中の座標
  val xInChar = RegInit(0.asUInt(3.W))
  val yInChar = RegInit(0.asUInt(4.W))

  val charRom = Module(new CharRom)
  charRom.io.clka := clock
  charRom.io.ena := true.B
  charRom.io.addra := io.charData.bits.charCode * 16.U + yInChar

  val pxShiftReg = Module(new ShiftRegisterPISO(8))
  pxShiftReg.io.d := charRom.io.douta
  pxShiftReg.io.load := state === sShiftRegLoad
  pxShiftReg.io.enable := state === sVramWrite

  // ステート・マシン
  switch (state) {
    is (sIdle) {
      when (io.charData.valid) {
        state := sRomRead
        yInChar := 0.U
      }
    }
    is (sRomRead) {
      state := sShiftRegLoad
    }
    is (sShiftRegLoad) {
      state := sVramWrite
      xInChar := 0.U
    }
    is (sVramWrite) {
      when (xInChar === 7.U) {
        when (yInChar === 15.U) {
          state := sIdle
        } .otherwise {
          state := sRomRead
          yInChar := yInChar + 1.U
        }
      } .otherwise {
        xInChar := xInChar + 1.U
      }
    }
  }

  // 文字の左上の点のVRAMアドレス
  val pxBaseAddr = (mode.hDispMax * 16).U * io.charData.bits.row + 8.U * io.charData.bits.col
  io.vramData.bits.addr := pxBaseAddr + mode.hDispMax.U * yInChar + xInChar
  io.vramData.bits.data := Mux(pxShiftReg.io.shiftOut, io.charData.bits.color, 0.U)
  io.vramData.valid := state === sVramWrite

  io.charData.ready := state === sIdle
}
