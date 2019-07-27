// See LICENSE for license details.

import chisel3._
import chisel3.util._

object ALU {
  /* Function codes. Constant definition.
   */
  val FUNC_ADD                    = "b0000".U  // addition
  val FUNC_SUBTRACT               = "b1010".U  // subtraction

  val FUNC_SHIFT_LEFT             = "b0001".U  // left shift
  val FUNC_SHIFT_RIGHT            = "b0101".U  // logical right shift

  val FUNC_XOR                    = "b0100".U  // eXcrusive OR
  val FUNC_OR                     = "b0110".U  // logical OR
  val FUNC_AND                    = "b0111".U  // logical AND

  val FUNC_SET_EQUAL              = "b0010".U  // Equality(==)
  val FUNC_SET_NOT_EQ             = "b0011".U  // Inequality(!=)

  val FUNC_SET_LESS_THAN          = "b1101".U  // Less Than(<)

  /** Bit width of function code */
  val FUNC_WIDTH = 4.W  

  /** return true if subtraction is required */
  def isSubtract(command: UInt) = command(3)

  def isComp(command: UInt) = command >= FUNC_SET_LESS_THAN

  /** return true if NOT operation to result of comparison is required */
  def compInverted(command: UInt) = command(0)

  /** return true if equality operation */
  def compEq(command: UInt) = !command(3)
}

// If we import member of ALU object, we can abbreviate "ALU.".
import ALU._

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

  // addition, subtraction
  val in2_inv = Mux(isSubtract(io.func), ~io.in2, io.in2)
  io.adderOut := io.in1 + in2_inv + isSubtract(io.func)

  // comparison(less than, equality, inequality)
  val in1_xor_in2 = io.in1 ^ in2_inv
  val setLessThan = Mux(io.in1(3) === io.in2(3), io.adderOut(3), io.in1)
  io.compResult := compInverted(io.func) ^ Mux(compEq(io.func), in1_xor_in2 === 0.U, setLessThan)

  // shift operation
  val (shiftAmount, shiftInR) = (io.in2(2, 0), io.in1)
  val shiftIn = Mux(io.func === FUNC_SHIFT_RIGHT, shiftInR, Reverse(shiftInR))
  val shiftOutR = shiftInR >> shiftAmount
  val shiftOutL = Reverse(shiftOutR)
  val shiftOut = Mux(io.func === FUNC_SHIFT_RIGHT, shiftOutR, 0.U) |
                 Mux(io.func === FUNC_SHIFT_LEFT,  shiftOutL, 0.U)

  // bitwise operation("A XOR B | A AND B" is same as "A OR B")
  val logicOut = Mux(io.func === FUNC_XOR || io.func === FUNC_OR, in1_xor_in2, 0.U) |
                 Mux(io.func === FUNC_OR || io.func === FUNC_AND, io.in1 & io.in2, 0.U)

  // result of less than, shift, bitwise operation
  val compShiftLogic = (isComp(io.func) & setLessThan) | logicOut | shiftOut

  // whole result
  io.out := Mux(io.func === FUNC_ADD || io.func === FUNC_SUBTRACT, io.adderOut, compShiftLogic)
}

object ALUApp extends App {
  chisel3.Driver.execute(args, () => new ALU)
}
