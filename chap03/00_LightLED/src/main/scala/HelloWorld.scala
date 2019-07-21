// See LICENSE for license details.

/**
  * Class of displaying "Hello, word!"
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
  * Companion object to HelloWorld class
  */
object HelloWorld {
  def main(args: Array[String]) = {
    val helloWorld = new HelloWorld()
    helloWorld.printMessage("John")
    println(helloWorld.getMessage)
  }
}
