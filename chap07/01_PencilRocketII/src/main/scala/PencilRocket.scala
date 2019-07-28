// See LICENSE for license details.

import chisel3._
import chisel3.util._

import chisel3.core.withClock

import scala.collection.mutable.ArrayBuffer

object util {
  implicit def uintToBitPat(x: UInt): BitPat = BitPat(x)
}

import util._

object ScalarOpConstants {

  def X = BitPat("b?")
  def N = BitPat("b0") // Bit pattern of No
  def Y = BitPat("b1") // Bit pattern of Yes

  // Type of ALU input1
  def A1_X    = BitPat("b??")
  def A1_ZERO = 0.U(2.W) // Specify zero register
  def A1_RS1  = 1.U(2.W) // Value of register of rs1
  def A1_PC   = 2.U(2.W) // Value of program counter

  // Type of ALU input2
  def A2_X    = BitPat("b??")
  def A2_SIZE = 1.U(2.W) // Size in bytes of one instruction
  def A2_RS2  = 2.U(2.W) // Value of register of rs2
  def A2_IMM  = 3.U(2.W) // Immidate value described in instruction

  def IMM_X  = BitPat("b???")
  def IMM_S  = UInt(0, 3) // Immidate value of S Type instruction
  def IMM_SB = UInt(1, 3) // Immidate value of B Type instruction
  def IMM_U  = UInt(2, 3) // Immidate value of U Type instruction
  def IMM_UJ = UInt(3, 3) // Immidate value of J Type instruction
  def IMM_I  = UInt(4, 3) // Immidate value of I Type instruction
  def IMM_Z  = UInt(5, 3) // Immidate value of Z Type instruction (used in system management instruction)

  // Size of memory access
  val MT_SZ = 3.W
  def MT_X  = BitPat("b???")
  def MT_B  = "b000".U
}

/** Constant class for memory operation
  */
object MemoryOpConstants {
  val NUM_XA_OPS = 9.W
  val M_SZ      = 5.W

  def M_X       = BitPat("b?????")
  def M_XRD     = UInt("b00000"); // Read Integer
  def M_XWR     = UInt("b00001"); // Write Integer
}

object ALU {
  // Function codes. Constant definition.
  val SZ_ALU_FN = 4.W
  def FN_ADD  = UInt(0)  // addition
  def FN_SL   = UInt(1)  // left shift
  def FN_SEQ  = UInt(2)  // Equality(==)
  def FN_SNE  = UInt(3)  // Inequality(!=)
  def FN_XOR  = UInt(4)  // eXcrusive OR
  def FN_SR   = UInt(5)  // logical right shift
  def FN_OR   = UInt(6)  // logical OR
  def FN_AND  = UInt(7)  // logical AND
  def FN_SUB  = UInt(10) // subtraction
  def FN_SRA  = UInt(11) // arithmetic right shift
  def FN_SLT  = UInt(12) // Less Than(<)
  def FN_SGE  = UInt(13) // Greater than or Equal(>=)
  def FN_SLTU = UInt(14) // Less Than(<) for Unsigned value
  def FN_SGEU = UInt(15) // Greater than or Equal(>=) for Unsigned value

  /** return true if subtraction is required */
  def isSub(cmd: UInt) = cmd(3)

  /** whether comparation */
  def isCmp(cmd: UInt) = cmd >= FN_SLT

  /** whetehr comparation of unsigned integer */
  def cmpUnsigned(cmd: UInt) = cmd(1)

  /** return true if NOT operation to result of comparison is required */
  def cmpInverted(cmd: UInt) = cmd(0)

  /** return true if equality operation */
  def cmpEq(cmd: UInt) = !cmd(3)
}

import ScalarOpConstants._
import MemoryOpConstants._
import ALU._

/** a CPU core of "PencilRocket II".
  * This is a pipeline 32-bit CPU that supports RISC-V instructions that can display "Hello, world".
  */
/** PencilRocket IIのCPUコアです。
  * Hello, worldが表示できる程度のRISC-V命令をサポートした、パイプライン化32ビットCPUです。
  */
class PencilRocketII extends Module {
  val io = IO(new Bundle {
    val iData = Flipped(new IDataBundle)
    val dData = Flipped(new DDataBundle)
  })

  /*
   * Constant definition
   */
  val pc_ini = "h8000_0000".U // Inital value of program counter (start at 0x8000_0000)
  val npc_ini = "h8000_0004".U // Inital value of next program counter (start at 0x8000_0004)
  val nop_inst = 19.U // nop instruction (same as "addi zero, zero, 0". Initial register value of pipeline stage)
  val nop_ctrl = (new IntCtrlSigs).decode(nop_inst, (new IDecode).table) // Control signal of nop

  /*
   * Register declaration of pipeline
   */
  // For instruction decode stage
  val id_pc = RegInit(pc_ini)
  val id_npc = RegInit(npc_ini)
  val id_inst = RegInit(nop_inst)

  // For execution stage
  val ex_pc = RegInit(pc_ini)
  val ex_npc = RegInit(npc_ini)
  val ex_ctrl = RegInit(nop_ctrl)
  val ex_inst = RegInit(nop_inst)
  val ex_reg_raddr = RegInit(VecInit(0.U(5.W), 0.U(5.W)))
  val ex_reg_waddr = RegInit(0.U(5.W))
  val ex_rs = RegInit(VecInit(0.U(32.W), 0.U(32.W)))

  // For memory read/write stage
  val mem_pc = RegInit(pc_ini)
  val mem_npc = RegInit(npc_ini)
  val mem_ctrl = RegInit(nop_ctrl)
  val mem_rs = RegInit(VecInit(0.U(32.W), 0.U(32.W)))
  val mem_reg_waddr = RegInit(0.U(5.W))
  val mem_imm = RegInit(0.S(32.W))
  val mem_alu_out = RegInit(0.U(32.W))
  val mem_alu_cmp_out = RegInit(false.B)

  // For write back register stage
  val wb_npc = RegInit(npc_ini)
  val wb_ctrl = RegInit(nop_ctrl)
  val wb_reg_waddr = RegInit(0.U(32.W))
  val wb_alu_out = RegInit(0.U(32.W))
  val wb_dData_readData = RegInit(0.U(32.W))

  /*
   * For stall
   */
  val load_stall = Wire(Bool())

  /*
   * For branch instruction
   */
  val jump_flush = Wire(Bool())

  /*****************************************
   * Program counter generation
   *****************************************/
  val pc = RegInit(UInt(32.W), pc_ini)  // Program counter
  val npc = pc + 4.U // Next instruction address

  /*****************************************
   * Instruction Fetch
   *****************************************/
  io.iData.addr := pc

  /*****************************************
   * Instruction Decode
   *****************************************/
  when (!load_stall && !jump_flush) {
    id_pc := pc
    id_npc := npc
    id_inst := io.iData.inst
  } .elsewhen (jump_flush) {
    id_pc := pc_ini
    id_npc := npc_ini
    id_inst := nop_inst
  }

  val ibuf = Module(new IBuf)
  ibuf.io.imem := id_inst

  // generate internal control signal
  val id_ctrl = Wire(new IntCtrlSigs).decode(ibuf.io.inst.bits, (new IDecode).table)

  // Register address
  val id_raddr2 = ibuf.io.inst.rs2
  val id_raddr1 = ibuf.io.inst.rs1
  val id_waddr  = ibuf.io.inst.rd
  val id_raddr = IndexedSeq(id_raddr1, id_raddr2) // Handle read addresses together

  // Read register file
  // The clock signal is inverted to perform register writing at the falling edge of the CPU clock.
  val revClock = Wire(new Clock)
  revClock := ~(clock.asUInt())
  val rf = new RegFile
  val id_rs = id_raddr.map(rf.read _) // Sequence of read register values

  // need to stall?
  load_stall := ((id_raddr(0) === ex_reg_waddr || id_raddr(1) === ex_reg_waddr)
    && ex_ctrl.mem === Y && ex_ctrl.mem_cmd === M_XRD)

  /*****************************************
   * Execute
   *****************************************/
  when (!load_stall && !jump_flush) {
    ex_pc := id_pc
    ex_npc := id_npc
    ex_ctrl := id_ctrl
    ex_inst := ibuf.io.inst.bits
    ex_reg_raddr := id_raddr
    ex_reg_waddr := id_waddr
    ex_rs := id_rs
  } .otherwise {
    ex_pc := pc_ini
    ex_npc := npc_ini
    ex_ctrl := nop_ctrl
    ex_inst := nop_inst
    ex_reg_raddr := VecInit(0.U, 0.U)
    ex_reg_waddr := 0.U
    ex_rs := VecInit(0.U, 0.U)
  }

  val ex_rs_bypassed = for (i <- 0 until id_raddr.size) yield MuxCase(ex_rs(i), Seq(
      (ex_reg_raddr(i) =/= 0.U && ex_reg_raddr(i) === mem_reg_waddr && mem_ctrl.wxd === Y) ->
        mem_alu_out,
      (ex_reg_raddr(i) =/= 0.U && ex_reg_raddr(i) === wb_reg_waddr  && wb_ctrl.wxd  === Y &&
        wb_ctrl.mem === Y) -> io.dData.readData))

  val ex_imm = ImmGen(ex_ctrl.sel_imm, ex_inst)

  // Determine the value to be input to ALU
  val ex_op1 = MuxLookup(ex_ctrl.sel_alu1, 0.U(32.W), Seq(
    A1_RS1 -> ex_rs_bypassed(0),
    A1_PC -> ex_pc))
  val ex_op2 = MuxLookup(ex_ctrl.sel_alu2, 0.U(32.W), Seq(
    A2_RS2 -> ex_rs_bypassed(1),
    A2_IMM -> ex_imm.asUInt,
    A2_SIZE -> 4.U(32.W)))

  // ALU
  val alu = Module(new ALU)
  alu.io.fn := ex_ctrl.alu_fn
  alu.io.in2 := ex_op2
  alu.io.in1 := ex_op1

  /*****************************************
   * Read and write to memory
   *****************************************/
  when (!jump_flush) {
    mem_pc := ex_pc
    mem_npc := ex_npc
    mem_ctrl := ex_ctrl
    mem_rs := ex_rs_bypassed
    mem_reg_waddr := ex_reg_waddr
    mem_imm := ex_imm
    mem_alu_out := alu.io.out
    mem_alu_cmp_out := alu.io.cmp_out
  } .otherwise {
    mem_pc := pc_ini
    mem_npc := npc_ini
    mem_ctrl := nop_ctrl
    mem_rs := VecInit(0.U, 0.U)
    mem_reg_waddr := 0.U
    mem_imm := 0.S
    mem_alu_out := 0.U
    mem_alu_cmp_out := false.B
  }

  io.dData.addr := mem_alu_out
  io.dData.writeEnable := mem_ctrl.mem_cmd === M_XWR
  io.dData.size := mem_ctrl.mem_type
  io.dData.writeData := mem_rs(1)

  jump_flush := (mem_ctrl.branch === Y && mem_alu_cmp_out ||
    mem_ctrl.jal === Y ||
    mem_ctrl.jalr === Y)

  /*****************************************
   * Write Back
   *****************************************/
  wb_npc := mem_npc
  wb_ctrl := mem_ctrl
  wb_reg_waddr := mem_reg_waddr
  wb_alu_out := mem_alu_out
  wb_dData_readData := io.dData.readData

  val revClock = Wire(new Clock)
  revClock := (~(clock.asUInt.toBool)).asClock
  withClock(revClock) {
    val rf_wen = wb_ctrl.wxd
    val rf_waddr = wb_reg_waddr
    val rf_wdata = MuxCase(wb_alu_out, Seq(
      (wb_ctrl.jalr === Y) -> wb_npc,
      (wb_ctrl.mem  === Y) -> wb_dData_readData))
    when (rf_wen) { rf.write(rf_waddr, rf_wdata) }
  }

  /*****************************************
   * Program counter update
   *****************************************/
  when (!load_stall) {
    pc := MuxCase(npc, Seq(
      (mem_ctrl.branch === Y && mem_alu_cmp_out ||
        mem_ctrl.jal === Y) -> (mem_pc + mem_imm.asUInt),
      (mem_ctrl.jalr === Y) -> mem_alu_out))
  }
}

/** Micorcomputer module with PencilRocket II CPU.
  * (Chiba is the second place where a real pencil rocket did a horizontal launch experiment.)
  */
class Chiba extends Module {
  val io = IO(new Bundle {
    val rxData = Input(Bool())
    val txData = Output(Bool())
    val rts = Output(Bool())
    val cts = Input(Bool())
  })

  val slowClock = Module(new SlowClock)
  slowClock.io.clk_system := clock

  withClock (slowClock.io.clk_slow) {
    val cpu = Module(new PencilRocketII)

    val iData = Module(new IData(Program.iData))
    iData.io <> cpu.io.iData

    val dData = Module(new DData(Program.dData))
    when ("h80001000".U <= cpu.io.dData.addr && cpu.io.dData.addr < "h80002000".U) {
      dData.io.addr := cpu.io.dData.addr
      dData.io.size := cpu.io.dData.size
      dData.io.writeData := cpu.io.dData.writeData
      dData.io.writeEnable := cpu.io.dData.writeEnable
    } .otherwise {
      dData.io.addr := 0.U
      dData.io.size := 0.U
      dData.io.writeData := 0.U
      dData.io.writeEnable := false.B
    }

    val uart = Module(new Uart)
    uart.io.rxData := io.rxData
    io.txData := uart.io.txData
    io.rts := uart.io.rts
    uart.io.cts := io.cts
    when ("h10000000".U === cpu.io.dData.addr) {
      uart.io.sendData.bits := cpu.io.dData.writeData
      uart.io.sendData.valid := cpu.io.dData.writeEnable
    } .otherwise {
      uart.io.sendData.bits := 0.U
      uart.io.sendData.valid := false.B
    }
    uart.io.receiveData.ready := false.B

    when ("h10000000".U === cpu.io.dData.addr) {
      cpu.io.dData.readData := 0.U
    } .elsewhen ("h80001000".U <= cpu.io.dData.addr && cpu.io.dData.addr < "h80002000".U) {
      cpu.io.dData.readData := dData.io.readData
    } .otherwise {
      cpu.io.dData.readData := 0.U
    }
  }
}

/** Register file
  * Since this is not used as a module, it is just a class.
  */
class RegFile {
  // Register file body
  val rf = Mem(32, UInt(32.W))

  /** Read a value of register
    * @param addr Read address(0 - 31)
    */
  def read(addr: UInt) = {
    Mux(addr === 0.U, 0.U(32.W), rf(addr))
  }

  /** Write a value to register
    * @param addr Write address(0 - 31)
    * @param data value to write(32 bit)
    */
  def write(addr: UInt, data: UInt) {
    when (addr =/= 0.U) {
      rf(addr) := data
    }
  }
}

/** Bundle for Instruction memory
  */
class IDataBundle extends Bundle {
  val addr = Input(UInt(32.W))  // プログラム・カウンタのアドレス
  val inst = Output(UInt(32.W)) // 機械語の命令
}

/** Instruction memory. Configure as ROM.
  * @param inits Instruction code(Up to 4KB. 1024 instructions)
  * Int type may be a negative number, which causes an error when converted to UInt, so it is a Long type.
  */
class IData(insts: List[Long]) extends Module {
  val io = IO(new IDataBundle)

  // Max count of Instructions
  val wordMax = 1024

  // Contents of ROM. Fill to 1024 instructions. Padding with zero.
  val image = insts ::: List.fill(wordMax - insts.length)(0L)

  // ROM
  val rom = VecInit(image.map((n) => n.asUInt(32.W)))

  // The lower 2 bits are ignored and accessed with 4-byte alignment.
  io.inst := rom(io.addr(log2Ceil(wordMax) - 1, 2))
}

/** Bundle for Data memory
  */
class DDataBundle extends Bundle {
  val addr = Input(UInt(32.W))
  val size = Input(UInt(2.W)) // Size of data(0: 1 byte, 1: 2 byte, 2: 4 byte.)
  val writeData = Input(UInt(32.W))
  val writeEnable = Input(Bool())
  val readData = Output(UInt(32.W))
}

/** Data memory. (4KB Capacity)
  * @param inits Initial data(32bit. store with little endian).
  * Int type may be a negative number, which causes an error when converted to UInt, so it is a Long type.
  */
class DData(inits: List[Long]) extends Module {
  val io = IO(new DDataBundle)

  // Max count of Data
  val wordMax = 1024

  // Access data size
  val bAccess = 0.U // 1 byte access
  val hAccess = 1.U // 2 byte access
  val wAccess = 2.U // 4 byte access

  // Initial contents of RAM. Fill to 1024 instructions. Padding with zero.
  val image = inits ::: List.fill(wordMax - inits.length)(0L)

  // RAM
  val ram = RegInit(VecInit(image.map((n) => n.asUInt(32.W))))

  // Data of specified address. The lower 2 bits are ignored and accessed with 4-byte alignment.
  val accessWord = ram(io.addr(log2Ceil(wordMax) - 1, 2))

  /*
   * Read. 
   */ 
  when (io.size === bAccess) {
    io.readData := Cat("h000".U,
      // Determine the bit field to be read by the lower 2 bits of addr
      MuxLookup(io.addr(1, 0), 0.U(8.W), Array(
        0.U -> accessWord(7, 0),
        1.U -> accessWord(15, 8),
        2.U -> accessWord(23, 16),
        3.U -> accessWord(31, 24))))
  } .elsewhen (io.size === hAccess) {
    io.readData := Cat("h00".U,
      // Bit 1 of addr determines which bit field to read
      MuxLookup(io.addr(1), 0.U(16.W), Array(
        0.U -> accessWord(15, 0),
        1.U -> accessWord(31, 16))))
  } .otherwise {
    io.readData := accessWord
  }

  /*
   * Write
   */
  when (io.writeEnable) {
    when (io.size === bAccess) {
      val bData = io.writeData(7, 0)
      accessWord := MuxLookup(io.addr(1, 0), 0.U(32.W), Array(
        0.U -> Cat(accessWord(31, 8),  bData),
        1.U -> Cat(accessWord(31, 16), bData, accessWord(7, 0)),
        2.U -> Cat(accessWord(31, 24), bData, accessWord(15, 0)),
        3.U -> Cat(bData,              accessWord(23, 0))))
    } .elsewhen (io.size === hAccess) {
      val hData = io.writeData(15, 0)
      accessWord := MuxLookup(io.addr(1), 0.U(32.W), Array(
        0.U -> Cat(accessWord(31, 16), hData),
        1.U -> Cat(hData,              accessWord(15, 0))))
    } .otherwise {
      accessWord := io.writeData
    }
  }
}

/** Bundle of instruction code and register addresses
  */
class ExpandedInstruction extends Bundle {
  val bits = UInt(width = 32)  // Instruction code
  val rd = UInt(5.W)     // Destination register address (register number)
  val rs1 = UInt(5.W)    // Source register 1 address
  val rs2 = UInt(5.W)    // Source register 2 address
}

/** RISC-V instruction decode class
  * @param x machine code
  */
class RVCDecoder(x: UInt) {
  /** Return bundle of instruction code and register address
    */
  def inst(bits: UInt, rd: UInt = x(11,7), rs1: UInt = x(19,15), rs2: UInt = x(24,20)) = {
    val res = Wire(new ExpandedInstruction)
    res.bits := bits
    res.rd := rd
    res.rs1 := rs1
    res.rs2 := rs2
    res
  }
}

/** Module to decode instruction memory
  */
class IBuf extends Module {
  val io = IO(new Bundle {
    val imem = Input(UInt(32.W))
    val inst = Output(new ExpandedInstruction)
  })

  io.inst := new RVCDecoder(io.imem).inst(io.imem)
}

/** Bit pattern of instruction code
  */
object Instructions {
  def BEQ                = BitPat("b?????????????????000?????1100011")
  def JALR               = BitPat("b?????????????????000?????1100111")
  def JAL                = BitPat("b?????????????????????????1101111")
  def LUI                = BitPat("b?????????????????????????0110111")
  def AUIPC              = BitPat("b?????????????????????????0010111")
  def ADDI               = BitPat("b?????????????????000?????0010011")
  def LB                 = BitPat("b?????????????????000?????0000011")
  def SB                 = BitPat("b?????????????????000?????0100011")
}

import Instructions._

/** Instruction -> Internal control signal
  */
class IDecode {
  val table: Array[(BitPat, List[BitPat])] = Array(
                //     jal                                                                 
                //     | jalr                                                              
                //     | |         s_alu1                 mem_val             
                //   br| | s_alu2  |       imm    alu     | mem_cmd   
                //   | | | |       |       |      |       | |     mem_type
                //   | | | |       |       |      |       | |     |     wxd
                //   | | | |       |       |      |       | |     |     |
    BEQ->       List(Y,N,N,A2_RS2, A1_RS1, IMM_SB,FN_SEQ, N,M_X,  MT_X, N),
    JALR->      List(N,N,Y,A2_IMM, A1_RS1, IMM_I, FN_ADD, N,M_X,  MT_X, Y),
    JAL->       List(N,Y,N,A2_SIZE,A1_PC,  IMM_UJ,FN_ADD, N,M_X,  MT_X, Y),
    LUI->       List(N,N,N,A2_IMM, A1_ZERO,IMM_U, FN_ADD, N,M_X,  MT_X, Y),
    AUIPC->     List(N,N,N,A2_IMM, A1_PC,  IMM_U, FN_ADD, N,M_X,  MT_X, Y),
    ADDI->      List(N,N,N,A2_IMM, A1_RS1, IMM_I, FN_ADD, N,M_X,  MT_X, Y),
    LB->        List(N,N,N,A2_IMM, A1_RS1, IMM_I, FN_ADD, Y,M_XRD,MT_B, Y),
    SB->        List(N,N,N,A2_IMM, A1_RS1, IMM_S, FN_ADD, Y,M_XWR,MT_B, N)
  )
}

/** Internal control signal bundle
  */
class IntCtrlSigs extends Bundle {
  val branch = Bool() // Conditional branch instruction(br)
  val jal = Bool()    // jump instruction(jal)
  val jalr = Bool()   // jump instruction with value of register(jalr)
  val sel_alu2 = Bits(width = A2_X.getWidth) // Source data type of input2 of ALU(s_alu2)
  val sel_alu1 = Bits(width = A1_X.getWidth) // Source data tyep of input1 of ALU(s_alu1)
  val sel_imm = Bits(width = IMM_X.getWidth) // type of imm
  val alu_fn = Bits(width = FN_X.getWidth)   // function requested to ALU(alu)
  val mem = Bool()                           // Whether memory access instruction(mem_val)
  val mem_cmd = Bits(width = M_SZ)           // Kind of memory access(mem_cmd)
  val mem_type = Bits(width = MT_SZ)         // memory access size(mem_type)
  val wxd = Bool()                           // wxd

  def default: List[BitPat] =
                //     jal                                                                 
                //     | jalr                                                              
                //     | |         s_alu1                   mem_val             
                //   br| | s_alu2  |       imm    alu       | mem_cmd   
                //   | | | |       |       |      |         | |    mem_type
                //   | | | |       |       |      |         | |    |     wxd
                //   | | | |       |       |      |         | |    |     |
                List(X,X,X,A2_X,   A1_X,   IMM_X, FN_X,     N,M_X, MT_X, X)

  /** Generate a circuit to set fields such as "branch" and "jal" from machine code values.
    */
  def decode(inst: UInt, table: Iterable[(BitPat, List[BitPat])]) = {
    val decoder = DecodeLogic(inst, default, mappingIn = table)

    // Connect the value returned by DecodeLogic to the corresponding field
    // Same as below.
    //  branch := decoder(0)
    //  jal    := decoder(1)
    //  ...
    val sigs = Seq(branch, jal, jalr, sel_alu2,
                   sel_alu1, sel_imm, alu_fn, mem, mem_cmd, mem_type, wxd)
    sigs zip decoder map {case(s,d) => s := d}

    this // Return this Bundle itself
  }
}

/**
  * Generate an internal control signal from the bit pattern of RISV-V machine code
  */
object DecodeLogic {
  /** 
    * @param addr Machine code
    * @param mappinIn Sequence of control signal corresponding to bit pattern of instruction
    */
  def apply(addr: UInt, default: Seq[BitPat], mappingIn: Iterable[(BitPat, Seq[BitPat])]): Seq[UInt] = {
    val mapping = ArrayBuffer.fill(default.size)(ArrayBuffer[(BitPat, BitPat)]())

    // Reorder from
    // Array(BEQ-> List(Y, ...,A2_RS2, A1_RS1, ...), JALR-> List(N, ...,A2_IMM, A1_RS1, ...), ...) 
    // To 
    // ArrayBuffer(ArrayBuffer(BEQ -> Y, JALR -> N, ...),
    //             ...
    //             ArrayBuffer(BEQ -> A2_RS2, JALR -> A2_IMM, ...),
    //             ArrayBuffer(BEQ -> A1_RS1, JALR -> A1_RS1, ...), ...)
    for ((key, values) <- mappingIn)
      for ((value, i) <- values zipWithIndex)
        mapping(i) += key -> value

    for ((thisDefault, thisMapping) <- default zip mapping)
      yield apply(addr, thisDefault, thisMapping)
  }

  /** Generate One control signal from machine code
    * @param addr Machine code
    * @param mappinIn Sequence of control signal corresponding to bit pattern of instruction
    */
  def apply(addr: UInt, default: BitPat, mapping: Iterable[(BitPat, BitPat)]): UInt = {
    // Convert to below form
    // MuxCase(default.value, Seq(
    //   addr === BEQ -> A2_RS2,
    //   addr === JALR -> A2_IMM, ...))
    MuxCase(default.value.U,
      mapping.map{ case (instBitPat, ctrlSigBitPat) => (addr === instBitPat) -> ctrlSigBitPat.value.U }.toSeq)
  }
}

/** Generate immidate value
  */
object ImmGen {
  /** Generate immediate value from machine code
    * @param sel Instruction format. Constant IMM_X defined ScalarOpConstants trait
    * @param inst machine code instruction
    * 
    * @return 32 bit width signed integer
    */
  def apply(sel: UInt, inst: UInt) = {
    val sign = Mux(sel === IMM_Z, 0.S, inst(31).asSInt)
    val b30_20 = Mux(sel === IMM_U, inst(30,20).asSInt, sign)
    val b19_12 = Mux(sel =/= IMM_U && sel =/= IMM_UJ, sign, inst(19,12).asSInt)
    val b11 = Mux(sel === IMM_U || sel === IMM_Z, 0.S,
              Mux(sel === IMM_UJ, inst(20).asSInt,
              Mux(sel === IMM_SB, inst(7).asSInt, sign)))
    val b10_5 = Mux(sel === IMM_U || sel === IMM_Z, 0.U, inst(30,25))
    val b4_1 = Mux(sel === IMM_U, 0.U,
               Mux(sel === IMM_S || sel === IMM_SB, inst(11,8),
               Mux(sel === IMM_Z, inst(19,16), inst(24,21))))
    val b0 = Mux(sel === IMM_S, inst(7),
             Mux(sel === IMM_I, inst(20),
             Mux(sel === IMM_Z, inst(15), 0.U)))

    Cat(sign, b30_20, b19_12, b11, b10_5, b4_1, b0).asSInt
  }
}

/** ALU
  */
class ALU extends Module {
  val io = IO(new Bundle {
    val fn = Input(Bits(SZ_ALU_FN)) // Type of calcuration
    val in2 = Input(UInt(32.W))     // operand1
    val in1 = Input(UInt(32.W))     // operand2
    val out = Output(UInt(32.W))    // result of calcuration
    val adder_out = Output(UInt(32.W)) // result of addition
    val cmp_out = Output(Bool())       // result of comparison
  })

  // addition, subtraction
  val in2_inv = Mux(isSub(io.fn), ~io.in2, io.in2)
  val in1_xor_in2 = io.in1 ^ in2_inv
  io.adder_out := io.in1 + in2_inv + isSub(io.fn)

  // comparison(less than, equality, inequality)
  val slt =
    Mux(io.in1(31) === io.in2(31), io.adder_out(31),
    Mux(cmpUnsigned(io.fn), io.in2(31), io.in1(31)))
  io.cmp_out := cmpInverted(io.fn) ^ Mux(cmpEq(io.fn), in1_xor_in2 === 0.U, slt)

  // shift operation
  val (shamt, shin_r) = (io.in2(4,0), io.in1)
  val shin = Mux(io.fn === FN_SR  || io.fn === FN_SRA, shin_r, Reverse(shin_r))
  val shout_r = (Cat(isSub(io.fn) & shin(31), shin).asSInt >> shamt)(31,0)
  val shout_l = Reverse(shout_r)
  val shout = Mux(io.fn === FN_SR || io.fn === FN_SRA, shout_r, 0.U) |
              Mux(io.fn === FN_SL,                     shout_l, 0.U)

  // bitwise operation("A XOR B | A AND B" is same as "A OR B")
  val logic = Mux(io.fn === FN_XOR || io.fn === FN_OR, in1_xor_in2, 0.U) |
              Mux(io.fn === FN_OR || io.fn === FN_AND, io.in1 & io.in2, 0.U)
  val shift_logic = (isCmp(io.fn) && slt) | logic | shout
  val out = Mux(io.fn === FN_ADD || io.fn === FN_SUB, io.adder_out, shift_logic)

  io.out := out
}

/** Program Data
  */
object Program {
  val iData = List(0x00001417L, 0x00040413L, 0x00040503L, 0x00140413L,
    0x00050c63L, 0x008000efL, 0xff1ff06fL, 0x100002b7L,
    0x00a28023L, 0x00008067L, 0x0000006fL)

  val dData = List(0x6c6c6548L, 0x77202c6fL, 0x646c726fL, 0x00000a21L)
}

/** Reduce the clock frequency
  */
class SlowClock extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk_system = Input(Clock())
    val clk_slow = Output(Clock())
  })
}

/** Object to generate a Velilog file of a Microcomputer with PencilRocket Core
  */
object PencilRocketPC extends App {
  chisel3.Driver.execute(args, () => new Chiba)
}
