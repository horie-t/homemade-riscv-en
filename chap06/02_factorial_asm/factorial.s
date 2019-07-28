        .section .text          # Program body section

        .equ        UART_ADDR, 0x10000000        # UART I/O Address
	
	.global _start
_start:
        la sp, sp_top           # Set the stack pointer value to the sp_top address defined in linker.ld
        li a0, 5                # n = 5
        jal factorial           # Call factorial(5)
        jal sendnum             # Display return value
        j fin
	
# calc factorial function
factorial:
        addi sp, sp, -16        # Allocate space on the stack
        sd a0, 8(sp)            # save a0
        sd ra, 0(sp)            # save ra
	
        li t0, 2                # Is it smaller than 2?(We really want to find out if it is less than 1)
        slt t0, a0, t0          # a0 < 2 (a0 <= 1)
        beqz t0, fact_else      # Branch to fact_else if a0 is 2 or more
        li a0, 1                # Since a0 is 1 or less, the return value is 1
	
        addi sp, sp, 16         # Release the allocated space.
	ret
fact_else:	
        addi a0, a0, -1         # Set arguments for recursive calls
        jal factorial           # Recursive call: factorial(n-1)
        mv t0, a0               # Save return value to temporary register
	
        ld ra, 0(sp)            # restore ra
        ld a0, 8(sp)            # restore a0
        addi sp, sp, 16         # Release the allocated space.
        mul a0, a0, t0          # calculate "n * factorial(n-1)"
	ret
	
# Function to send number
sendnum:
        addi sp, sp, -16        # Allocate space on the stack
        sd a0, 8(sp)            # save a0
        sd ra, 0(sp)            # save ra
	
        li t0, 10
        div t1, a0, t0          # Check if a0 is 9 or less(t1 = a0 / 10)
        bnez t1, sendnum_else
        # Display a character because a0 is 9 or less
        li t0, 48               # ASCII code of '0'
        add a0, a0, t0          # Calculate the ASCII code of a number(a0 = s0 + '0')
        jal sendchar            # Display one-digit character
	
        ld ra, 0(sp)            # restore ra
        addi sp, sp, 16         # Release the allocated space.
	ret
sendnum_else:
	mv a0, t1
        jal sendnum             # Recursive call
	
        ld a0, 8(sp)            # restore a0
        li t0, 10
        rem a0, a0, t0          # Calculate number of least significant digit(a0 = a0 % 10)
        li t0, 48               # ASCII code of '0'
        add a0, a0, t0          # Calculate the ASCII code of a number(a0 = s0 + '0')
        jal sendchar            # Display one-digit character
	
        ld ra, 0(sp)            # restore ra
        addi sp, sp, 16         # Release the allocated space.
	ret

# one character transmission function
sendchar:
        li t0, UART_ADDR        # Load immediate value of UART I/O address
        sb a0, 0(t0)            # Write a0 to the address of UART
        ret                     # Return
	
# finish
fin:
        j fin                   # Infinite loop doing nothing
