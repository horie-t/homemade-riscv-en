	.section .text		# プログラム本体セクション

	.equ	UART_ADDR, 0x10000000	# UARTのI/Oアドレス
	
	.global _start
_start:
	la sp, sp_top		# スタック・ポインタのアドレスをlinker.ldで設定しているsp_topに設定
	li a0, 5		# n = 4
	jal factorial		# factorial(4)として呼び出す
	jal sendnum		# 戻り値を表示させる
	j fin
	
# 階乗計算関数
factorial:
	addi sp, sp, -16   	# スタックに領域を確保
	sd a0, 8(sp)       	# a0を退避
	sd ra, 0(sp)		# raを退避
	
	li t0, 2		# 2より小さいか(本当は1以下かを調べたい)
	slt t0, a0, t0		# a0 < 2 (a0 <= 1)
	beqz t0, fact_else      # 2以上ならfact_elseへ
	li a0, 1		# 1以下なので戻り値は1
	
	addi sp, sp, 16         # スタックを元に戻す
	ret
fact_else:	
	addi a0, a0, -1		# 再帰呼出しの引数をセット
	jal factorial		# 再帰呼出し: factorial(n-1)
	mv t0, a0		# 戻り値を一時領域に退避
	
	ld ra, 0(sp)		# raを復帰
	ld a0, 8(sp)		# a0を復帰
	addi sp, sp, 16         # スタックを元に戻す
	mul a0, a0, t0		# n * factorial(n-1)を計算
	ret
	
# 数値の送信関数
sendnum:
	addi sp, sp, -16   	# スタックに領域を確保
	sd a0, 8(sp)       	# a0を退避
	sd ra, 0(sp)		# raを退避
	
	li t0, 10
	div t1, a0, t0		# 9以下かを調べたい(t1 = a0 / 10)
	bnez t1, sendnum_else
	# 9以下なので、文字を表示
	li t0, 48		# '0'のASCIIコード
	add a0, a0, t0		# 数値のASCIIコードを算出(a0 = s0 + '0')
	jal sendchar		# 1桁分文字を表示
	
	ld ra, 0(sp)		# raを復帰
	addi sp, sp, 16         # スタックを元に戻す
	ret
sendnum_else:
	mv a0, t1
	jal sendnum		# 再帰呼出し
	
	ld a0, 8(sp)		# a0を復帰
	li t0, 10
	rem a0, a0, t0		# 1の位の数値を算出(a0 = a0 % 10)
	li t0, 48		# '0'のASCIIコード
	add a0, a0, t0		# 数値のASCIIコードを算出(a0 = s0 + '0')
	jal sendchar		# 1桁分文字を表示
	
	ld ra, 0(sp)		# raを復帰
	addi sp, sp, 16         # スタックを元に戻す
	ret

# 1文字送信関数
sendchar:
	li t0, UART_ADDR	# UARTのI/Oアドレスの即値をロード
	sb a0, 0(t0)		# UARTのアドレスへa0を書き込む
	ret
	
# 終了
fin:	
	j fin			# 何もしない無限ループ
