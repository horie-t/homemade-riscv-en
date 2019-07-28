# Hello, world! on QEMU(RISC-V assembly language version)

CC=riscv64-unknown-elf-gcc
TARGET=hello

all:	$(TARGET)

$(TARGET):	$(TARGET).s
	$(CC) -march=rv64g -mabi=lp64 -nostartfiles -Tlinker.ld $< -o $@

run:
	@echo "Press Ctrl + a, x to terminate"
	qemu-system-riscv64 -M virt -kernel $(TARGET) -nographic

.PHONY:	clean
clean:
	-rm -f $(TARGET) *~
