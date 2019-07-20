// UARTのI/Oアドレス
volatile unsigned char * const UART_ADDR = (unsigned char *)0x10000000;

// 1文字送信関数
void sendchar(int c)
{
  *UART_ADDR = c;
}

int main(void)
{
  char const *s = "Hello world!\n";
  int c;
  
  while ((c = *s++) != '\0') {
    sendchar(c);
  }
  
  return 0;
}

