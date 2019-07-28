	.section .text
	
	.global _start
_start:
        la sp, sp_top        # Set stack pointer which is defined in "linker.ld" file
        jal main             # call main function
# finish
fin:	
        j fin                # Infinite loop doing nothing
