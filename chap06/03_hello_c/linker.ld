OUTPUT_ARCH( "riscv" )		/* RISV-V用のバイナリを出力する */
ENTRY(_start)			/* _startのコードから実行開始 */

SECTIONS
{
  . = 0x80000000;		/* 0x80000000番地にコードを配置する */
  startup : { startup.o(.text) }
  .text : { *(.text) }		/* 機械語のコードを配置 */
  .data : { *(.data) }		/* 静的データを配置 */
  .rodata : { *(.rodata) }	/* 読み込み専用の静的データを配置 */
  .bss : { *(.bss) }		/* 初期化なしの静的データを配置 */
  . = ALIGN(8);
  . = . + 0x1000;
  sp_top = .;			/* スタックのポインタ初期アドレス */
}
