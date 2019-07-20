// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** 7セグメントLED用のバンドル
  */
class Seg7LEDBundle extends Bundle {
  /** 各セグメントの点灯用。0〜7をCAからCGに対応させる事。0の時に点灯、1の時に消灯します。 */
  val cathodes = UInt(7.W)
  /** 小数点用。0の時に点灯、1の時に消灯。 */
  val decimalPoint = UInt(1.W)
  /** 桁の選択用。0の桁が点灯、１の桁が消灯。 */
  val anodes = UInt(8.W)
}

/**
  * 7セグメントLED点灯モジュール(1桁版)
  */
class Seg7LED1Digit extends Module {
  val io = IO(new Bundle {
    val num = Input(UInt(4.W))
    val seg7led = Output(new Seg7LEDBundle) // 引数を取らないコンストラクタなので型名の後の括弧は省略してみた
  })

  io.seg7led.cathodes := MuxCase("b111_1111".U, // デフォルト値(全部消灯)。16すべてのパターンが定義されるので使われない。
    Seq(                    // gfe_dcba の順序にcathodeが並ぶ
      (io.num === "h0".U) -> "b100_0000".U,
      (io.num === "h1".U) -> "b111_1001".U,
      (io.num === "h2".U) -> "b010_0100".U,
      (io.num === "h3".U) -> "b011_0000".U,
      (io.num === "h4".U) -> "b001_1001".U,
      (io.num === "h5".U) -> "b001_0010".U,
      (io.num === "h6".U) -> "b000_0010".U,
      (io.num === "h7".U) -> "b101_1000".U,
      (io.num === "h8".U) -> "b000_0000".U,
      (io.num === "h9".U) -> "b001_0000".U,
      (io.num === "ha".U) -> "b000_1000".U,
      (io.num === "hb".U) -> "b000_0011".U,
      (io.num === "hc".U) -> "b100_0110".U,
      (io.num === "hd".U) -> "b010_0001".U,
      (io.num === "he".U) -> "b000_0110".U,
      (io.num === "hf".U) -> "b000_1110".U))

  io.seg7led.decimalPoint := 1.U         // 小数点は点灯させない。
  io.seg7led.anodes := "b1111_1110".U    // 0桁目だけ点灯させる
}

object Seg7LED1Digit extends App {
  chisel3.Driver.execute(args, () => new Seg7LED1Digit)
}
