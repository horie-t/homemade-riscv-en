// See LICENSE for license details.

import chisel3._

/**
  * Compare operation
  */
class IntCompare extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W)) // Input A
    val b = Input(UInt(4.W)) // Input B
    val bit_ope = Output(Bool())
    val bit_reduction = Output(Bool())
    val equal_ope = Output(Bool())
    val equal_5 = Output(Bool())
    val not_5 = Output(Bool())
  })

  // Compare by bit, if there is even one difference, they are not equal
  io.bit_ope := ~(io.a(0) ^ io.b(0) | io.a(1) ^ io.b(1) | io.a(2) ^ io.b(2) | io.a(3) ^ io.b(3))

  // Bitwise comparison of "a" and "b" can be done like "a ^ b".
  // Since it is hard to write like "a (0) | a (1) | a (2) | a (3)", the ". orR" operator is defined.
  io.bit_reduction := ~((io.a ^ io.b).orR)

  // Since Chisel also has an equality operator, it does not actually do the troublesome thing like the above.
  io.equal_ope := io.a === io.b

  // You can also compare with numeric literals. Match the missing bits to the larger one.
  io.equal_5 := io.a === 5.U

  // Inequality operator is "=/=".
  io.not_5 := io.a =/= 5.U
}

object IntCompare extends App {
  chisel3.Driver.execute(args, () => new IntCompare())
}
