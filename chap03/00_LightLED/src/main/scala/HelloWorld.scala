// See LICENSE for license details.

/**
  * 「Hello, word!」を表示するクラス
  */
class HelloWorld {
  private val message: String = "Hello, world!"
  def printMessage(name: String): Unit = {
    println(message + " Hello, " + name + "!")
  }
  def getMessage: String = {
    message
  }
}

/**
  * HelloWorldクラスのコンパニオン(対になる)オブジェクト
  */
object HelloWorld {
  def main(args: Array[String]) = {
    val helloWorld = new HelloWorld()
    helloWorld.printMessage("John")
    println(helloWorld.getMessage)
  }
}
