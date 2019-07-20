	.section .text
	
	.global _start
_start:
	la sp, sp_top		# スタック・ポインタのアドレスをlinker.ldで設定しているsp_topに設定
	jal main		# main関数を呼び出す
# 終了
fin:	
	j fin			# main関数からもどって来て、何もしない無限ループ
