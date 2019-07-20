// See LICENSE for license details.

import chisel3._
import chisel3.util._

/** キッチンタイマー
  */
class KitchenTimer extends Module {
  val io = IO(new Bundle {
    val min = Input(Bool())
    val sec = Input(Bool())
    val startStop = Input(Bool())
    val seg7led = Output(new Seg7LEDBundle)
  })

  /* ステートマシン定義 */
  //  時間設定  :: タイマー稼働 :: 一時停止 :: タイマー終了
  val sTimeSet :: sRun :: sPause :: sFin :: Nil = Enum(4)
  val state = RegInit(sTimeSet)
  val stateChange = Debounce(io.startStop) // スタート・ストップボタンは、実際は状態遷移イベント

  // 時間管理
  val time = Module(new Time)
  // 時間の変更は、設定状態か、一時停止状態のみ可能
  time.io.incMin := (state === sTimeSet || state === sPause) && Debounce(io.min)
  time.io.incSec := (state === sTimeSet || state === sPause) && Debounce(io.sec)
  time.io.countDown := state === sRun

  // ステートマシン遷移処理
  when (stateChange) {
    switch (state) {
      is (sTimeSet) {
        when (!time.io.zero) {
          state := sRun
        }
      }
      is (sRun) {
        state := sPause
      }
      is (sPause) {
        state := sRun
      }
      is (sFin)   {
        state := sTimeSet
      }
    }
  } .elsewhen (state === sRun && time.io.zero) {
    state := sFin
  }

  // 出力
  val seg7LED = Module(new Seg7LED)
  seg7LED.io.digits := VecInit(List(time.io.sec % 10.U, (time.io.sec / 10.U)(3, 0),
    time.io.min % 10.U, (time.io.min / 10.U)(3, 0)) ::: List.fill(4) { 0.U(4.W) })
  seg7LED.io.blink := (state === sFin)
  io.seg7led := seg7LED.io.seg7led
}

object KitchenTimer extends App {
  chisel3.Driver.execute(args, () => new KitchenTimer)
}
