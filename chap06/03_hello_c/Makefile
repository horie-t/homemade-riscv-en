# QEMU(RISC-V版)での、アセンブリ言語でのHello, world!

CC=riscv64-unknown-elf-gcc
TARGET=hello

all:	$(TARGET)

$(TARGET):	$(TARGET).o startup.o
	riscv64-unknown-elf-ld -static -nostartfiles -T linker.ld -o $@ $^

$(TARGET).o:	$(TARGET).c
	$(CC) -march=rv64g -mabi=lp64 -mcmodel=medany -o $@ -c $<

startup.o: 	startup.s
#	$(CC) -march=rv64g -mabi=lp64 -T linker.ld -o $@ -c $<
	$(CC) -march=rv64g -mabi=lp64 -o $@ -c $<

run:
	@echo "終了するには、Ctrl + a, x を押して下さい"
	qemu-system-riscv64 -M virt -kernel $(TARGET) -nographic

.PHONY:	clean
clean:
	-rm -f $(TARGET) *.o *~
