// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** UART
  */
class Uart extends Module {
  val io = IO(new Bundle {
    val rxData = Input(Bool())
    val txData = Output(Bool())
    val rts = Output(Bool())
    val cts = Input(Bool())
    val receiveData = Decoupled(UInt(8.W))
    val sendData = Flipped(Decoupled(UInt(8.W)))
  })

  val uartRx = Module(new UartRx)
  uartRx.io.rxData := io.rxData
  io.receiveData <> uartRx.io.receiveData

  val txQueue = Module(new Queue(UInt(8.W), 64))
  txQueue.io.enq <> io.sendData

  val uartTx = Module(new UartTx)
  io.txData := uartTx.io.txData
  uartTx.io.sendData <> txQueue.io.deq

  io.rts := false.B
}

/** UART Transmitter
  */
class UartTx extends Module {
  val io = IO(new Bundle {
    val sendData = Flipped(Decoupled(UInt(8.W)))
    val txData   = Output(Bool())
  })

  /* State machine definition */
  val sIdle :: sStartBit :: sSend :: sStopBit :: Nil = Enum(4)
  val state = RegInit(sIdle)

  // Counter for state transition
  val (txCount, txEnable) = Counter(true.B, 25000000 / 115200)
  // Counter for 8-bit transmission
  val sendCount = Reg(UInt(3.W))
  // Shift register for transmission data
  val shiftReg = Module(new ShiftRegisterPISO(8))
  shiftReg.io.d      := Reverse(io.sendData.bits)
  shiftReg.io.load   := state === sIdle && io.sendData.valid
  shiftReg.io.enable := state === sSend && txEnable

  // State transition
  when (state === sIdle && io.sendData.valid)  {
    state := sStartBit
    txCount := 0.U
  } .elsewhen (txEnable) {
    switch (state) {
      is (sStartBit) {
        state := sSend
        sendCount := 0.U
      }
      is (sSend) {
        when (sendCount === 7.U) {
          state := sStopBit
        } .otherwise {
          sendCount := sendCount + 1.U
        }
      }
      is (sStopBit) {
        state := sIdle
      }
    }
  }

  io.sendData.ready := state === sIdle
  
  io.txData := MuxCase(true.B, Array(
    (state === sStartBit) -> false.B,
    (state === sSend)     -> shiftReg.io.shiftOut,
    (state === sStopBit)  -> true.B))
}

/** UART receiver
  */
class UartRx extends Module {
  val io = IO(new Bundle {
    val rxData = Input(Bool())
    val receiveData = Decoupled(UInt(8.W))
  })

  /* State machine definition */
  val sIdle :: sStartBit :: sReceive :: sStopBit :: Nil = Enum(4)
  val state = RegInit(sIdle)

  // Counter for state transition 
  val (rxCount, rxEnable) = Counter(true.B, 25000000 / 115200)
   // Counter for half a cycle of 1 bit
  val (rxHalfCount, rxHalfEnable) = Counter(true.B, 25000000 / 115200 / 2)
  // Counter for 8-bit receiving
  val receiveCount = Reg(UInt(3.W))

  val shiftReg = Module(new ShiftRegisterSIPO(8))
  shiftReg.io.shiftIn := io.rxData
  shiftReg.io.enable  := state === sReceive && rxEnable

  val rDataValid = RegInit(false.B)
  val start = NegEdge(Synchronizer(io.rxData))

  // State transition
  when (state === sIdle && start) {
    state := sStartBit
    rxHalfCount := 0.U
  } .elsewhen (state === sStartBit && rxHalfEnable) {
    state := sReceive
    rxCount := 0.U
    receiveCount := 0.U
  } .elsewhen (rxEnable) {
    switch (state) {
      is (sReceive) {
        when (receiveCount === 7.U) {
          state := sStopBit
        } .otherwise {
          receiveCount := receiveCount + 1.U
        }
      }
      is (sStopBit) {
        when (rxEnable) {
          state := sIdle
        }
      }
    }
  }

  when (state === sStopBit && rxEnable) {
    rDataValid := true.B
  } .elsewhen (io.receiveData.ready) {
    rDataValid := false.B
  }

  io.receiveData.bits := Reverse(shiftReg.io.q)
  io.receiveData.valid := rDataValid
}
