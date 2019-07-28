OUTPUT_ARCH( "riscv" )	 /* Output binary file for RISC-V */
ENTRY(_start)			 /* Start execution from code of _start */

SECTIONS
{
  . = 0x80000000;		/* Place code at address 0x80000000 */
  .text : { *(.text) }		/* Place machine code */
  .rodata : { *(.rodata) }	/* Place read-only static data */
  .data : { *(.data) }		/* Place static data */
  .bss : { *(.bss) }		/* Place static data without initialization */
  . = ALIGN(8);          /* If bss ends with an half-way address, make it an address that is a multiple of 8 */
  . = . + 0x1000;        /* Allocate 4KB for the stack */
  sp_top = .;            /* Stack pointer initial address */
}
