# Hello, world! on QEMU(RISC-V C language version)

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
	@echo "Press Ctrl + a, x to terminate"
	qemu-system-riscv64 -M virt -kernel $(TARGET) -nographic

.PHONY:	clean
clean:
	-rm -f $(TARGET) *.o *~
