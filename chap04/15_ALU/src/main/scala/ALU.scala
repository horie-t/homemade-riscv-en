// See LICENSE for license details.

import chisel3._
import chisel3.util._

object ALU {
  /*
   * 各種機能コード 定数定義
   */
  val FUNC_ADD                    = "b0000".U  // 加算の機能コード
  val FUNC_SUBTRACT               = "b1010".U  // 減算の機能コード

  val FUNC_SHIFT_LEFT             = "b0001".U  // 左シフトの機能コード
  val FUNC_SHIFT_RIGHT            = "b0101".U  // 論理右シフトの機能コード

  val FUNC_XOR                    = "b0100".U  // 排他的論理和(XOR)の機能コード
  val FUNC_OR                     = "b0110".U  // 論理和(OR)の機能コード
  val FUNC_AND                    = "b0111".U  // 論理積(AND)の機能コード

  val FUNC_SET_EQUAL              = "b0010".U  // 等号(==)判定の機能コード
  val FUNC_SET_NOT_EQ             = "b0011".U  // 不等(==)判定の機能コード

  val FUNC_SET_LESS_THAN          = "b1101".U  // 未満(<)の機能コード

  /** 機能コードのビット幅 */
  val FUNC_WIDTH = 4.W  

  /** 引き算の処理が必要かどうかを返します */
  def isSubtract(command: UInt) = command(3)

  def isComp(command: UInt) = command >= FUNC_SET_LESS_THAN

  /** 比較処理の結果にNOT演算が必要かどうかを返します */
  def compInverted(command: UInt) = command(0)

  /** 比較処理の内で、等しいまたは等しくないの演算かどうか */
  def compEq(command: UInt) = !command(3)
}

import ALU._ // ALUオブジェクトのメンバーをインポートする事により、ALUクラス内でALU.を省略できる。

/** ALU
  */
class ALU extends Module {
  val io = IO(new Bundle {
    val func = Input(UInt(FUNC_WIDTH))
    val in1 = Input(UInt(4.W))
    val in2 = Input(UInt(4.W))
    val out = Output(UInt(4.W))
    val adderOut = Output(UInt(4.W))
    val compResult = Output(Bool())
  })

  // 加算、減算
  val in2_inv = Mux(isSubtract(io.func), ~io.in2, io.in2)
  io.adderOut := io.in1 + in2_inv + isSubtract(io.func)

  // 各種比較(未満、等値、不等)
  val in1_xor_in2 = io.in1 ^ in2_inv
  val setLessThan = Mux(io.in1(3) === io.in2(3), io.adderOut(3), io.in1)
  io.compResult := compInverted(io.func) ^ Mux(compEq(io.func), in1_xor_in2 === 0.U, setLessThan)

  // シフト演算
  val (shiftAmount, shiftInR) = (io.in2(2, 0), io.in1)
  val shiftIn = Mux(io.func === FUNC_SHIFT_RIGHT, shiftInR, Reverse(shiftInR))
  val shiftOutR = shiftInR >> shiftAmount
  val shiftOutL = Reverse(shiftOutR)
  val shiftOut = Mux(io.func === FUNC_SHIFT_RIGHT, shiftOutR, 0.U) |
                 Mux(io.func === FUNC_SHIFT_LEFT,  shiftOutL, 0.U)

  // ビット演算(補足: A XOR B | A AND Bは、 A OR Bと同じ)
  val logicOut = Mux(io.func === FUNC_XOR || io.func === FUNC_OR, in1_xor_in2, 0.U) |
                 Mux(io.func === FUNC_OR || io.func === FUNC_AND, io.in1 & io.in2, 0.U)

  // 未満、シフト、ビット演算の結果
  val compShiftLogic = (isComp(io.func) & setLessThan) | logicOut | shiftOut

  // 全体としての結果
  io.out := Mux(io.func === FUNC_ADD || io.func === FUNC_SUBTRACT, io.adderOut, compShiftLogic)
}

object ALUApp extends App {
  chisel3.Driver.execute(args, () => new ALU)
}
