# 階乗計算

CC=riscv64-unknown-elf-gcc
TARGET=factorial

all:	$(TARGET)

$(TARGET):	$(TARGET).s
	$(CC) -march=rv64g -mabi=lp64 -nostartfiles -Tlinker.ld $< -o $@

run:
	@echo "終了するには、Ctrl + a, x を押して下さい"
	qemu-system-riscv64 -M virt -kernel $(TARGET) -nographic

.PHONY:	clean
clean:
	-rm -f $(TARGET) *~
