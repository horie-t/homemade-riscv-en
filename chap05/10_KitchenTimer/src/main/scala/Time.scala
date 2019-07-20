// See LICENSE for license details.

import chisel3._
import chisel3.util._

object Time {
  // 60進数表記(0〜59)に必要なビット幅
  val sexagesimalWitdh = (log2Floor(59) + 1).W
}

/** 時間のモデルクラス
  */
class Time extends Module {
  val io = IO(new Bundle {
    val incMin = Input(Bool()) // 時間を1分増加します。
    val incSec = Input(Bool()) // 時間を1秒増加します。
    val countDown = Input(Bool()) // カウントダウンを実行します。

    val min = Output(UInt(Time.sexagesimalWitdh)) // 残りの分。
    val sec = Output(UInt(Time.sexagesimalWitdh)) // 残りの秒。
    val zero = Output(Bool())                     // 残り時間がなくなったかどうか
  })

  val min = RegInit(0.U(Time.sexagesimalWitdh))
  when (io.incMin) {
    when (min === 59.U) {
      min := 0.U
    } .otherwise {
      min := min + 1.U
    }
  }

  val sec = RegInit(0.U(Time.sexagesimalWitdh))
  when (io.incSec) {
    when (sec === 59.U) {
      sec := 0.U
    } .otherwise {
      sec := sec + 1.U
    }
  }

  val zero = Wire(Bool())
  val (count, oneSec) = Counter(io.countDown && !zero, 100000000) // 1秒未満の時間のカウンター
  zero := min === 0.U && sec === 0.U && count === 0.U

  when (io.countDown && oneSec) {
    when (sec === 0.U) {
      when (min =/= 0.U) {
        min := min - 1.U
        sec := 59.U
      }
    } .otherwise {
      sec := sec - 1.U
    }
  }

  io.min := min
  io.sec := sec
  io.zero := zero
}
