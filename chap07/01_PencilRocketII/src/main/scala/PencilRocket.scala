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
  def N = BitPat("b0") // Noのビット・パターン
  def Y = BitPat("b1") // Yesのビット・パターン

  // ALU入力1に入力する値
  def A1_X    = BitPat("b??")
  def A1_ZERO = 0.U(2.W) // ゼロ・レジスタを指定
  def A1_RS1  = 1.U(2.W) // rs1に指定されたレジスタの値
  def A1_PC   = 2.U(2.W) // プログラム・カウンタの値

  // ALU入力2に入力する値
  def A2_X    = BitPat("b??")
  def A2_SIZE = 1.U(2.W) // 1命令のバイト数
  def A2_RS2  = 2.U(2.W) // rs2に指定されたレジスタの値
  def A2_IMM  = 3.U(2.W) // 命令の中の即値

  def IMM_X  = BitPat("b???")
  def IMM_S  = 0.U(3.W) // S形式の即値である
  def IMM_SB = 1.U(3.W) // B形式の即値である
  def IMM_U  = 2.U(3.W) // U形式の即値である
  def IMM_UJ = 3.U(3.W) // J形式の即値である
  def IMM_I  = 4.U(3.W) // I形式の即値である
  def IMM_Z  = 5.U(3.W) // Z形式(システム管理命令で使用)の即値である

  // メモリ・アクセスサイズ
  val MT_SZ = 3.W
  def MT_X  = BitPat("b???")
  def MT_B  = "b000".U // バイト単位アクセス
}

object MemoryOpConstants {
  val NUM_XA_OPS = 9.W
  val M_SZ      = 5.W

  def M_X       = BitPat("b?????")
  def M_XRD     = "b00000".U; // int load
  def M_XWR     = "b00001".U; // int store
}

object ALU {
  // ALUの演算機能コード
  val SZ_ALU_FN = 4.W
  def FN_X    = BitPat("b????")
  def FN_ADD  = 0.U  // 加算
  def FN_SL   = 1.U  // 左シフト
  def FN_SEQ  = 2.U  // 等号(==)判定
  def FN_SNE  = 3.U  // 不等(!=)判定
  def FN_XOR  = 4.U  // 排他的論理和(XOR)
  def FN_SR   = 5.U  // 右論理シフト
  def FN_OR   = 6.U  // 論理和(OR)
  def FN_AND  = 7.U  // 論理積(AND)
  def FN_SUB  = 10.U // 減算
  def FN_SRA  = 11.U // 右算術シフト
  def FN_SLT  = 12.U // 未満(<)判定
  def FN_SGE  = 13.U // 以上(>=)判定
  def FN_SLTU = 14.U // 未満(<)判定。符号なし
  def FN_SGEU = 15.U // 以上(>=)判定。符号なし

  /** 引き算の処理が必要かどうかを返します */
  def isSub(cmd: UInt) = cmd(3)

  /** 比較演算かどうか */
  def isCmp(cmd: UInt) = cmd >= FN_SLT

  /** 符号なし整数の比較か */
  def cmpUnsigned(cmd: UInt) = cmd(1)

  /** 比較処理の結果にNOT演算が必要かどうかを返します */
  def cmpInverted(cmd: UInt) = cmd(0)

  /** 比較処理の内で、等しいまたは等しくないの演算かどうか */
  def cmpEq(cmd: UInt) = !cmd(3)
}

import ScalarOpConstants._
import MemoryOpConstants._
import ALU._

/** PencilRocket IIのCPUコアです。
  * Hello, worldが表示できる程度のRISC-V命令をサポートした、パイプライン化32ビットCPUです。
  */
class PencilRocketII extends Module {
  val io = IO(new Bundle {
    val iData = Flipped(new IDataBundle)
    val dData = Flipped(new DDataBundle)
  })

  /*
   * 定数定義
   */
  val pc_ini = "h8000_0000".U // プログラムカウンタ初期値(0x8000_0000から開始する)
  val npc_ini = "h8000_0004".U // 次のプログラムカウンタ初期値(0x8000_0004から開始する)
  val nop_inst = 19.U // nop命令(addi zero, zero, 0。パイプラインの初期値とする。)
  val nop_ctrl = Wire((new IntCtrlSigs)).decode(nop_inst, (new IDecode).table) // nop命令の制御信号

  /*
   * パイプライン化用レジスタ宣言
   */
  // 命令デコードステージ用
  val id_pc = RegInit(pc_ini)
  val id_npc = RegInit(npc_ini)
  val id_inst = RegInit(nop_inst)

  // 実行ステージ用
  val ex_pc = RegInit(pc_ini)
  val ex_npc = RegInit(npc_ini)
  val ex_ctrl = RegInit(nop_ctrl)
  val ex_inst = RegInit(nop_inst)
  val ex_reg_raddr = RegInit(VecInit(0.U(5.W), 0.U(5.W)))
  val ex_reg_waddr = RegInit(0.U(5.W))
  val ex_rs = RegInit(VecInit(0.U(32.W), 0.U(32.W)))

  // データメモリ読み書きステージ用
  val mem_pc = RegInit(pc_ini)
  val mem_npc = RegInit(npc_ini)
  val mem_ctrl = RegInit(nop_ctrl)
  val mem_rs = RegInit(VecInit(0.U(32.W), 0.U(32.W)))
  val mem_reg_waddr = RegInit(0.U(5.W))
  val mem_imm = RegInit(0.S(32.W))
  val mem_alu_out = RegInit(0.U(32.W))
  val mem_alu_cmp_out = RegInit(false.B)

  // レジスタ書き戻しステージ用
  val wb_npc = RegInit(npc_ini)
  val wb_ctrl = RegInit(nop_ctrl)
  val wb_reg_waddr = RegInit(0.U(32.W))
  val wb_alu_out = RegInit(0.U(32.W))
  val wb_dData_readData = RegInit(0.U(32.W))

  /*
   * ストール処理用
   */
  val load_stall = Wire(Bool())

  /*
   * 分岐処理用
   */
  val jump_flush = Wire(Bool())

  /*****************************************
   * プログラム・カウンタ生成処理(PC Generate)
   *****************************************/
  val pc = RegInit(UInt(32.W), pc_ini)  // プログラム・カウンタ
  val npc = pc + 4.U // 次の命令アドレス

  /*****************************************
   * 命令取り出し処理(Instruction Fetch)
   *****************************************/
  io.iData.addr := pc

  /*****************************************
   * 命令デコード処理(Instruction Decode)
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

  // 内部制御信号生成
  val id_ctrl = Wire(new IntCtrlSigs).decode(ibuf.io.inst.bits, (new IDecode).table)

  // レジスタアドレス
  val id_raddr2 = ibuf.io.inst.rs2
  val id_raddr1 = ibuf.io.inst.rs1
  val id_waddr  = ibuf.io.inst.rd
  val id_raddr = IndexedSeq(id_raddr1, id_raddr2) // 読み出しアドレスをまとめて扱う

  // レジスタ・ファイル読み出し
  val rf = new RegFile
  val id_rs = id_raddr.map(rf.read _) // 読み出しレジスタの値のシーケンス

  // ストール判定
  load_stall := ((id_raddr(0) === ex_reg_waddr || id_raddr(1) === ex_reg_waddr)
    && ex_ctrl.mem === Y && ex_ctrl.mem_cmd === M_XRD)

  /*****************************************
   * 実行処理(Execute)
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

  // ALU入力決定
  val ex_op1 = MuxLookup(ex_ctrl.sel_alu1, 0.U(32.W), Seq(
    A1_RS1 -> ex_rs_bypassed(0),
    A1_PC -> ex_pc))
  val ex_op2 = MuxLookup(ex_ctrl.sel_alu2, 0.U(32.W), Seq(
    A2_RS2 -> ex_rs_bypassed(1),
    A2_IMM -> ex_imm.asUInt,
    A2_SIZE -> 4.U(32.W)))

  // 演算装置
  val alu = Module(new ALU)
  alu.io.fn := ex_ctrl.alu_fn
  alu.io.in2 := ex_op2
  alu.io.in1 := ex_op1

  /*****************************************
   * データメモリ読み書き処理(Memory)
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
   * レジスタ書き戻し処理(Write Back)
   *****************************************/
  wb_npc := mem_npc
  wb_ctrl := mem_ctrl
  wb_reg_waddr := mem_reg_waddr
  wb_alu_out := mem_alu_out
  wb_dData_readData := io.dData.readData

  // レジスタ書き込みは、CPUのクロック立ち上がりで行いたいので、クロック信号を反転する。
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
   * プログラム・カウンタの更新
   *****************************************/
  when (!load_stall) {
    pc := MuxCase(npc, Seq(
      (mem_ctrl.branch === Y && mem_alu_cmp_out ||
        mem_ctrl.jal === Y) -> (mem_pc + mem_imm.asUInt),
      (mem_ctrl.jalr === Y) -> mem_alu_out))
  }
}

/** PencilRocket II CPU搭載のパーソナル・コンピュータのモジュールです。
  * (本物のペンシルロケットが水平発射実験を行った2番目の地の名前にちなむ)
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

/** レジスタファイルです。
  * モジュールとしては利用しないので、ただのクラスになっています。
  * readやwriteはioのポートとして定義されないので、利用側の呼び出しの数だけ回路が形成されます。
  */
class RegFile {
  // レジスタファイル本体(zeroレジスタの分は余分だが確保)
  val rf = Mem(32, UInt(32.W))

  /** 読み出し
    * @param addr 読み出しアドレス(0〜31)
    */
  def read(addr: UInt) = {
    Mux(addr === 0.U, 0.U(32.W), rf(addr))
  }

  /** 書き込み
    * @param addr 書き込みアドレス(0〜31)
    * @param data 書き込みデータ(32ビット)
    */
  def write(addr: UInt, data: UInt) {
    when (addr =/= 0.U) {
      rf(addr) := data
    }
  }
}

/** 命令メモリ用バンドル
  */
class IDataBundle extends Bundle {
  val addr = Input(UInt(32.W))  // プログラム・カウンタのアドレス
  val inst = Output(UInt(32.W)) // 機械語の命令
}

/** 命令データメモリです。ROMとして実装しています。
  * @param insts 命令データ(4KBまで。1024命令まで保存可能)
  */
class IData(insts: List[Long]) extends Module {
  val io = IO(new IDataBundle)

   // 最大命令数
  val wordMax = 1024

  // ROMの元イメージ。1024命令に調整する。足りない分は0でパディング。
  val image = insts ::: List.fill(wordMax - insts.length)(0L)

  // ROM本体
  val rom = VecInit(image.map((n) => n.asUInt(32.W)))

  // 下位2ビットは無視して、4バイト単位でアクセスする。
  io.inst := rom(io.addr(log2Ceil(wordMax) - 1, 2))
}

/** データメモリ用バンドル
  */
class DDataBundle extends Bundle {
  val addr = Input(UInt(32.W))
  val size = Input(UInt(2.W)) // データサイズ(0: 1バイト、1: 2バイト、2: 4バイト。funct3の下位2ビット)
  val writeData = Input(UInt(32.W))
  val writeEnable = Input(Bool())
  val readData = Output(UInt(32.W))
}

/** データメモリです。(4KBの容量)
  * @param inits 初期データ(32bit、リトル・エンディアンで格納されます)
  */
class DData(inits: List[Long]) extends Module {
  val io = IO(new DDataBundle)

   // 最大データ数
  val wordMax = 1024

  // アクセスサイズ
  val bAccess = 0.U // 1バイトアクセス
  val hAccess = 1.U // 2バイトアクセス
  val wAccess = 2.U // 4バイトアクセス

  // RAMの初期イメージ。1024命令に調整する。
  val image = inits ::: List.fill(wordMax - inits.length)(0L)

  // RAM本体
  val ram = RegInit(VecInit(image.map((n) => n.asUInt(32.W))))

  // アドレス指定されたデータ
  val accessWord = ram(io.addr(log2Ceil(wordMax) - 1, 2))

  /*
   * 読み出し処理。下位2ビットは無視して、4バイト単位でアクセスする。
   */ 
  when (io.size === bAccess) {
    io.readData := Cat("h000".U,
      // 下位2ビット分で読み出すビット・フィールドを決定
      MuxLookup(io.addr(1, 0), 0.U(8.W), Array(
        0.U -> accessWord(7, 0),
        1.U -> accessWord(15, 8),
        2.U -> accessWord(23, 16),
        3.U -> accessWord(31, 24))))
  } .elsewhen (io.size === hAccess) {
    io.readData := Cat("h00".U,
      // ビット1で読み出すビット・フィールドを決定
      MuxLookup(io.addr(1), 0.U(16.W), Array(
        0.U -> accessWord(15, 0),
        1.U -> accessWord(31, 16))))
  } .otherwise {
    io.readData := accessWord
  }

  /*
   * 書き込み処理
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

/** 命令コードとレジスタアドレスの保持バンドル
  */
class ExpandedInstruction extends Bundle {
  val bits = UInt(32.W)  // 命令コード
  val rd = UInt(5.W)     // 宛先レジスタ・アドレス(レジスタ番号)
  val rs1 = UInt(5.W)    // 元レジスタ1・アドレス
  val rs2 = UInt(5.W)    // 元レジスタ2・アドレス
}

/** RISC-V命令コード・デコードクラス
  * @param x 機械語のビット列
  */
class RVCDecoder(x: UInt) {
  /** 命令コードとレジスタアドレスの保持バンドルを返します。
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

/** 命令メモリの内容のデコードモジュール
  */
class IBuf extends Module {
  val io = IO(new Bundle {
    val imem = Input(UInt(32.W))
    val inst = Output(new ExpandedInstruction)
  })

  io.inst := new RVCDecoder(io.imem).inst(io.imem)
}

/** 命令コードのビット・パターン
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

/** 命令コード -> 内部制御信号 対応定義
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

/** 内部制御信号バンドル
  */
class IntCtrlSigs extends Bundle {
  val branch = Bool() // 条件分岐命令(br)
  val jal = Bool()    // 無条件分岐命令(jal)
  val jalr = Bool()   // 無条件分岐命令(レジスタ指定)(jalr)
  val sel_alu2 = Bits(A2_X.getWidth.W) // ALUの入力2の元データタイプ(s_alu2)
  val sel_alu1 = Bits(A1_X.getWidth.W) // ALUの入力2の元データタイプ(s_alu1)
  val sel_imm = Bits(IMM_X.getWidth.W) // imm
  val alu_fn = Bits(FN_X.getWidth.W)   // ALUの呼び出し機能(alu)
  val mem = Bool()                     // メモリアクセス命令(mem_val)
  val mem_cmd = Bits(M_SZ)           // メモリアクセスの種類(mem_cmd)
  val mem_type = Bits(MT_SZ)         // メモリアクセスのサイズ(mem_type)
  val wxd = Bool()                   // レジスタ書き込みあり

  def default: List[BitPat] =
                //     jal                                                                 
                //     | jalr                                                              
                //     | |         s_alu1                   mem_val             
                //   br| | s_alu2  |       imm    alu       | mem_cmd   
                //   | | | |       |       |      |         | |    mem_type
                //   | | | |       |       |      |         | |    |     wxd
                //   | | | |       |       |      |         | |    |     |
                List(X,X,X,A2_X,   A1_X,   IMM_X, FN_X,     N,M_X, MT_X, X)

  /** 機械語の値から、branch、jal等フィールドを設定する回路を生成します。
    */
  def decode(inst: UInt, table: Iterable[(BitPat, List[BitPat])]) = {
    val decoder = DecodeLogic(inst, default, mappingIn = table)

    // DecodeLogicが返して来た値を各フィールドに対応付ける
    //  branch := decoder(0)
    //  jal    := decoder(1)
    //  … と同じ
    val sigs = Seq(branch, jal, jalr, sel_alu2,
                   sel_alu1, sel_imm, alu_fn, mem, mem_cmd, mem_type, wxd)
    sigs zip decoder map {case(s,d) => s := d}

    this // Bundle自身を返す
  }
}

/**
  * RISV-Vの機械語のビットパターンから、内部制御信号を生成
  */
object DecodeLogic {
  /** 機械語から、各種制御信号のシーケンスを返します。
    * @param addr  機械語
    * @param mappinIn 命令のビットパターンと、対応する制御信号のシーケンス
    */
  def apply(addr: UInt, default: Seq[BitPat], mappingIn: Iterable[(BitPat, Seq[BitPat])]): Seq[UInt] = {
    val mapping = ArrayBuffer.fill(default.size)(ArrayBuffer[(BitPat, BitPat)]())

    // Array(BEQ-> List(Y, ...,A2_RS2, A1_RS1, ...), JALR-> List(N, ...,A2_IMM, A1_RS1, ...), ...) の並びから、
    // ArrayBuffer(ArrayBuffer(BEQ -> Y, JALR -> N, ...),
    //             ...
    //             ArrayBuffer(BEQ -> A2_RS2, JALR -> A2_IMM, ...),
    //             ArrayBuffer(BEQ -> A1_RS1, JALR -> A1_RS1, ...), ...)
    // の形に並び替え
    for ((key, values) <- mappingIn)
      for ((value, i) <- values zipWithIndex)
        mapping(i) += key -> value

    for ((thisDefault, thisMapping) <- default zip mapping)
      yield apply(addr, thisDefault, thisMapping)
  }

  /** 1種類の制御信号を、機械語から生成する。
    * @param addr  機械語
    * @param mappinIn 命令のビットパターンと、対応する制御信号のシーケンス
    */
  def apply(addr: UInt, default: BitPat, mapping: Iterable[(BitPat, BitPat)]): UInt = {
    // MuxCase(default.value, Seq(
    //   addr === BEQ -> A2_RS2,
    //   addr === JALR -> A2_IMM, ...))
    // の形に変形
    MuxCase(default.value.U,
      mapping.map{ case (instBitPat, ctrlSigBitPat) => (addr === instBitPat) -> ctrlSigBitPat.value.U }.toSeq)
  }
}

/** 即値生成クラス
  */
object ImmGen {
  /** 命令から即値を生成します。
    * @param sel 命令形式。ScalarOpConstantsの、IMM_X形式の定数
    * @param inst 機械語命令
    * 
    * @return 32ビットの符号あり整数
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
    val fn = Input(UInt(SZ_ALU_FN)) // 演算の機能種類の指定
    val in2 = Input(UInt(32.W))     // 被演算数1
    val in1 = Input(UInt(32.W))     // 被演算数2
    val out = Output(UInt(32.W))    // 演算結果
    val adder_out = Output(UInt(32.W)) // 加減算結果
    val cmp_out = Output(Bool())       // 比較結果
  })

  // 加算、減算
  val in2_inv = Mux(isSub(io.fn), ~io.in2, io.in2)
  val in1_xor_in2 = io.in1 ^ in2_inv
  io.adder_out := io.in1 + in2_inv + isSub(io.fn)

  // 各種比較(未満、等値、不等)
  val slt =
    Mux(io.in1(31) === io.in2(31), io.adder_out(31),
    Mux(cmpUnsigned(io.fn), io.in2(31), io.in1(31)))
  io.cmp_out := cmpInverted(io.fn) ^ Mux(cmpEq(io.fn), in1_xor_in2 === 0.U, slt)

  // シフト演算
  val (shamt, shin_r) = (io.in2(4,0), io.in1)
  val shin = Mux(io.fn === FN_SR  || io.fn === FN_SRA, shin_r, Reverse(shin_r))
  val shout_r = (Cat(isSub(io.fn) & shin(31), shin).asSInt >> shamt)(31,0)
  val shout_l = Reverse(shout_r)
  val shout = Mux(io.fn === FN_SR || io.fn === FN_SRA, shout_r, 0.U) |
              Mux(io.fn === FN_SL,                     shout_l, 0.U)

  // ビット演算(補足: A XOR B | A AND Bは、 A OR Bと同じ)
  val logic = Mux(io.fn === FN_XOR || io.fn === FN_OR, in1_xor_in2, 0.U) |
              Mux(io.fn === FN_OR || io.fn === FN_AND, io.in1 & io.in2, 0.U)
  val shift_logic = (isCmp(io.fn) && slt) | logic | shout
  val out = Mux(io.fn === FN_ADD || io.fn === FN_SUB, io.adder_out, shift_logic)

  io.out := out
}

/** プログラムデータ
  */
object Program {
  val iData = List(0x00001417L, 0x00040413L, 0x00040503L, 0x00140413L,
    0x00050c63L, 0x008000efL, 0xff1ff06fL, 0x100002b7L,
    0x00a28023L, 0x00008067L, 0x0000006fL)

  val dData = List(0x6c6c6548L, 0x77202c6fL, 0x646c726fL, 0x00000a21L)
}

/** ボードのクロックでは動作しないので、クロックを下げるクラス。
  */
class SlowClock extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk_system = Input(Clock())
    val clk_slow = Output(Clock())
  })
}

/** PencilRocket Coreを搭載したパソコンのVelilogファイルを生成するオブジェクト
  */
object PencilRocketPC extends App {
  chisel3.Driver.execute(args, () => new Chiba)
}
