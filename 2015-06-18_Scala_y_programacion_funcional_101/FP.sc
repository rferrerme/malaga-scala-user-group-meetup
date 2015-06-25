object FP {

  // We will see functions as objects first
  var suma21: (Int, Int) => Int = (x: Int, y: Int) => x+y
                                                  //> suma21  : (Int, Int) => Int = <function2>
  suma21(4, 3)                                    //> res0: Int = 7

  // That is a function that receives an Int and returns a function that receives an Int and returns an Int
  val suma11: Int => Int => Int = (x: Int) => (y: Int) => x+y
                                                  //> suma11  : Int => (Int => Int) = <function1>

  val result1 = suma11(4)                         //> result1  : Int => Int = <function1>

  // That is a partial function

  result1(5)                                      //> res1: Int = 9

  suma21.curried                                  //> res2: Int => (Int => Int) = <function1>

  // That is a currified version of suma21

  suma21.curried(4)(5)                            //> res3: Int = 9

  suma21.tupled                                   //> res4: ((Int, Int)) => Int = <function1>

  suma21.tupled((4,5))                            //> res5: Int = 9

  // What we have done is representing functions using objects
  // That has a problem: functions cannot have generics
  // Another option is to use methods
  
  def suma22(x: Int, y: Int): Int = x+y           //> suma22: (x: Int, y: Int)Int
  
  // You can create an object from it
  
  suma22 _                                        //> res6: (Int, Int) => Int = <function2>
  
  def suma1(x: Int)(y: Int): Int = x+y            //> suma1: (x: Int)(y: Int)Int
  
  // It has blocks of parameters and you can do partials
  
  val result2 = suma1(5) _                        //> result2  : Int => Int = <function1>
  result2(7)                                      //> res7: Int = 12
  
  suma1 _                                         //> res8: Int => (Int => Int) = <function1>
  
  // That is a currified function
  
  // Generics
  
  def f[T](t: T): T = t                           //> f: [T](t: T)T
  
  // That is something that we cannot do with objects
  
  f[Int](3)                                       //> res9: Int = 3

  // You have to see that as two arguments, Int and 3, that helps
  
  // Very important: use pure functions

  def suma23(x: Int, y: Int): Int = {
    println("INFO: sumando " + x + "+" + y)
    x + y
  }                                               //> suma23: (x: Int, y: Int)Int
  
  suma23(3,4)                                     //> INFO: sumando 3+4
                                                  //| res10: Int = 7
  /*
  That is not a pure function. It does not declare everything it does. Pure functions can only do internal things

  Impure functions are evil:
    - Side effects make things difficult to understand
    - Testing is also difficult (you need mocks, etc)
    - Modularity and reusability are also a problem if there are side effects
    - Efficiency and scalability: pure functions can be executed in any order
    - But we cannot only have pure functions because someone has to do the job: database, web services, log...

  So how do we purify functions?
    - Logging has to be part of the result. It has to be declared in the signature, as a data type
  */
  
  // Purification
  
  trait LoggingInstruction
  case class Info(msg: String) extends LoggingInstruction
  case class Warn(msg: String) extends LoggingInstruction
  case class Debug(msg: String) extends LoggingInstruction
  
  Warn("Sumando 3 + 5")                           //> res11: FP.Warn = Warn(Sumando 3 + 5)
  
  Warn("Sumando 3 + 5"): LoggingInstruction       //> res12: FP.LoggingInstruction = Warn(Sumando 3 + 5)

  // We will return the log trace as a value

  def suma24(x: Int, y: Int): (LoggingInstruction, Int) = {
    val resultado = x + y
    (Info(s"$x + $y = $resultado"), resultado)
  }                                               //> suma24: (x: Int, y: Int)(FP.LoggingInstruction, Int)
  
  // That is the essence of function purification
  
  val result3 = suma24(4, 5)                      //> result3  : (FP.LoggingInstruction, Int) = (Info(4 + 5 = 9),9)
  
  // The result is a tuple
  
  result3._1                                      //> res13: FP.LoggingInstruction = Info(4 + 5 = 9)
  result3._2                                      //> res14: Int = 9
  
  (4,5): Tuple2[Int,Int]                          //> res15: (Int, Int) = (4,5)
  
  // To represent that we define a type alias
  
  type Logging[T] = (LoggingInstruction, T)
  
  // This is the same as before, but we are giving it a name

  def suma25(x: Int, y: Int): Logging[Int] = ???  //> suma25: (x: Int, y: Int)FP.Logging[Int]
  
  // That helps to understand it conceptually. The function returns an integer but decorated with logging instructions
  
  def suma26(x: Int, y: Int): Logging[Int] = (Info("ddd"), x+y)
                                                  //> suma26: (x: Int, y: Int)FP.Logging[Int]
  
  // In the real world every side effect should have its own type
  // Programs have the interpreter also to be able to deal with the side effects. We will have the pure part and the impure part as two separate parts

  // Testing should be easy then
  
  assert( suma26(4,5)._2 == 9 )
  assert( suma26(4,5)._1 == Info("ddd") )

  /*
  What is the gain?
    - Most part of the code can be tested, is modular, can be composed (but the other part has to exist also)
    - Our logging instructions can be implemented with println or log4j, etc
    - The business logic of my application will remain unaffected by that
    - The instructions set that I designed is not going to change
     - I have designed a language to define my effects

  All this is the Interpreter pattern (Gang of Four)
  */

  // My interpreter could remove the decoration and return the pure value

  def runIO(r: Logging[Int]): Int = {
    val (log, i) = r
    log match {
      case Info(msg) => println(s"INFO: $msg")
      case _ => println("otra instruccion")
    }
    i
  }                                               //> runIO: (r: FP.Logging[Int])Int
  
  // val (log, i) = r is called Extractor in Scala
  
  runIO( suma26(4,5) )                            //> INFO: ddd
                                                  //| res16: Int = 9
  
  // No side effect
  
  suma26(4,5)                                     //> res17: FP.Logging[Int] = (Info(ddd),9)
  
  /*
  E.g. a controller in the Play framework
  
    def controller(x: HTTPRequest): HTTPResult = ...
  
  That is not pure because it will have to go to the database, etc
  */
  
  // Composition: a functional program is a function composed of multiple functions
  
  def suma27(x: Int, y: Int): Int = x+y           //> suma27: (x: Int, y: Int)Int
  
  def negado1(x: Int): Int = { println("negando..."); -x }
                                                  //> negado1: (x: Int)Int
  
  negado1( suma27(4,5) )                          //> negando...
                                                  //| res18: Int = -9
  
  // I can do that because the signatures match
  
  // If I have decorated functions then I cannot match them
  
  def suma2(x: Int, y: Int): Logging[Int] = ???   //> suma2: (x: Int, y: Int)FP.Logging[Int]
  
  def negado(x: Int): Logging[Int] = ???          //> negado: (x: Int)FP.Logging[Int]
  
  // This generates an error: type mismatch
//  negado( suma2(4,5) )

  // How can we solve that problem?
  // We have to define a special compose operator (bind/flatMap)
  
  case class Multilog(logs: List[LoggingInstruction]) extends LoggingInstruction

  case class Logging2[T](log: LoggingInstruction, result: T) {

    // Special compose operator
    def bind[U](f: T => Logging2[U]): Logging2[U] = {
      val Logging2(log2, result2) = f(result)
      Logging2(Multilog(List(log, log2)), result2)
    }
  }

  def sumaPuraConLogging(x: Int, y: Int): Logging2[Int] = {
    val result = x + y
    Logging2(Info(s"$x + $y = $result"), result)
  }                                               //> sumaPuraConLogging: (x: Int, y: Int)FP.Logging2[Int]
  
  def negacionPuraConLogging(n: Int): Logging2[Int] = Logging2(Warn(s"-$n"), -n)
                                                  //> negacionPuraConLogging: (n: Int)FP.Logging2[Int]
  
  sumaPuraConLogging(3, 4) bind negacionPuraConLogging
                                                  //> res19: FP.Logging2[Int] = Logging2(Multilog(List(Info(3 + 4 = 7), Warn(-7))
                                                  //| ),-7)

  // Now it works and it contains all the information (result of the operation and composition of the logging effects)

  // Map

  case class Logging3[T](log: LoggingInstruction, result: T) {

    def bind[U](f: T => Logging3[U]): Logging3[U] = {
      val Logging3(log2, result2) = f(result)
      Logging3(Multilog(List(log, log2)), result2)
    }

    def map[U](f: T => U): Logging3[U] =
      Logging3(log, f(result))

  }

  def sumaPuraConLogging2(x: Int, y: Int): Logging3[Int] = {
    val resultado = x + y
    Logging3(Info(s"$x + $y = $resultado"), resultado)
  }                                               //> sumaPuraConLogging2: (x: Int, y: Int)FP.Logging3[Int]
     
  def negacionPura(n: Int): Int = -n              //> negacionPura: (n: Int)Int
  sumaPuraConLogging2(3,4) map negacionPura       //> res20: FP.Logging3[Int] = Logging3(Info(3 + 4 = 7),-7)
  
  // There are other composition operators: e.g. pure
  
  // pure will encapsulate a value in something with effects but the effect is empty
  
  def pure[T](t: T): Logging3[T] = Logging3(Multilog(List()), t)
                                                  //> pure: [T](t: T)FP.Logging3[T]
  
  val result4 = (negacionPura _) andThen pure     //> result4  : Int => FP.Logging3[Int] = <function1>
  
  result4(7)                                      //> res21: FP.Logging3[Int] = Logging3(Multilog(List()),-7)
}