	.section .text		# プログラム本体セクション

	.equ	UART_ADDR, 0x10000000	# UARTのI/Oアドレス
	
	.global _start
_start:
	la s0, message          # message文字列の先頭アドレスをロード
	
# 文字出力ループ
putloop: 
	lb a0, 0(s0)		# 先頭文字をa0にロード
	addi s0, s0, 1          # 文字列のアドレスを1文字進めておく
	beqz a0, fin		# NUL文字(0)ならfinに分岐する
	jal sendchar		# NUL文字ではないので、sendcharを呼び出す
	j putloop		# ループを繰り返す
	
# 1文字送信関数
sendchar:
	li t0, UART_ADDR	# UARTのI/Oアドレスの即値をロード
	sb a0, 0(t0)		# UARTのアドレスへa0を書き込む
	ret
	
# 終了
fin:	
	j fin			# 何もしない無限ループ

	.section .rodata	# 定数定義セクション
	
# 表示文字列データ
message:
	.ascii "Hello, world!\n\0"
	

