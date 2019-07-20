# QEMU(RISC-V版)での、アセンブリ言語でのHello, world!

CC=riscv64-unknown-elf-gcc
TARGET=hello

all:	$(TARGET)

$(TARGET):	$(TARGET).s
	$(CC) -march=rv64g -mabi=lp64 -nostartfiles -Tlinker.ld $< -o $@

run:
	@echo "終了するには、Ctrl + a, x を押して下さい"
	qemu-system-riscv64 -M virt -kernel $(TARGET) -nographic

.PHONY:	clean
clean:
	-rm -f $(TARGET) *~
