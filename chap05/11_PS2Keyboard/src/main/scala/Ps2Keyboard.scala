// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** PS/2 keyboard input
  */
class Ps2Keyboard extends Module {
  val io = IO(new Bundle {
    val ps2Clock = Input(Bool())
    val ps2Data = Input(Bool())
    val seg7led = Output(new Seg7LEDBundle)
  })

  // State transition signal
  val ps2ClkDown = NegEdge(Synchronizer(io.ps2Clock))

  val sIdle :: sReceive :: sParity :: Nil = Enum(3)
  val state = RegInit(sIdle)
  val receiveCount = Reg(UInt(3.W))

  when (ps2ClkDown) {
    switch (state) {
      is (sIdle) {
        when (io.ps2Data === false.B) {
          state := sReceive
          receiveCount := 0.U
        }
      }
      is (sReceive) {
        when (receiveCount === 7.U) {
          state := sParity
        } .otherwise {
          receiveCount := receiveCount + 1.U
        }
      }
      is (sParity) {
        state := sIdle
      }
    }
  }

  val receiveShiftReg = Module(new ShiftRegisterSIPO(8))
  receiveShiftReg.io.shiftIn := io.ps2Data
  receiveShiftReg.io.enable := state === sReceive && ps2ClkDown

  // Since "ShiftRegisterSIPO" has an old input with high-order bits, 
  // so reverse with "Reverse"
  val keyboadScanCode = RegEnable(Reverse(receiveShiftReg.io.q),
    0.U(8.W), state === sParity)

  // output
  val seg7LED = Module(new Seg7LED)
  seg7LED.io.digits := VecInit(List(keyboadScanCode(3, 0), keyboadScanCode(7, 4))
    ::: List.fill(6) { 0.U(4.W) })
  seg7LED.io.blink := false.B
  io.seg7led := seg7LED.io.seg7led
}

object Ps2Keyboard extends App {
  chisel3.Driver.execute(args, () => new Ps2Keyboard)
}
