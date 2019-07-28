OUTPUT_ARCH( "riscv" )		/* Output binary file for RISC-V */
ENTRY(_start)			/* Start execution from code of _start */

SECTIONS
{
  . = 0x80000000;		/* Place code at address 0x80000000 */
  .text : { *(.text) }		/* Place machine code */
  .rodata : { *(.rodata) }	/* Place read-only static data */
  .data : { *(.data) }		/* Place static data */
  .bss : { *(.bss) }		/* Place static data without initialization */
}
