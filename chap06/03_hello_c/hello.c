// UART I/O Address
volatile unsigned char * const UART_ADDR = (unsigned char *)0x10000000;

// one character transmission function
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

