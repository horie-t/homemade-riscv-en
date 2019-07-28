        .section .text          # Program body section

        .equ UART_ADDR, 0x10000000        # UART I/O Address
	
	.global _start
_start:
        la s0, message          # Load start address of message string
	
# Character output loop
putloop: 
        lb a0, 0(s0)            # Load first character to a0
        addi s0, s0, 1          # Advance the address of the string by one character
        beqz a0, fin            # Branch to fin if a0 is NUL character(0)
        jal sendchar            # Call sendchar because it is not a NUL character
        j putloop               # Repeat putloop
	
# one character transmission function
sendchar:
        li t0, UART_ADDR        # Load immediate value of UART I/O address
        sb a0, 0(t0)            # Write a0 to the address of UART
        ret                     # Return
	
# finish
fin:	
        j fin                   # Infinite loop doing nothing

        .section .rodata        # Constant definition section
	
# Display string data
message:
	.ascii "Hello, world!\n\0"
	

