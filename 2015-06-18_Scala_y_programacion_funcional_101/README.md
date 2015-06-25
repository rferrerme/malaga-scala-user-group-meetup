# "Scala y programación funcional 101"

Notes from the `Malaga Scala User Group` meetup on 2015-06-18: 

* Event: [http://www.meetup.com/Malaga-Scala/events/222982948/](http://www.meetup.com/Malaga-Scala/events/222982948/)
* Presenter: [Juan Manuel Serrano](https://github.com/jserranohidalgo)
* Presented slides: [https://docs.google.com/presentation/d/1JYhcSe3JcWdNQkz9oquX3vU5I8gKA-KWuzVPkaQbopk/edit?usp=sharing](https://docs.google.com/presentation/d/1JYhcSe3JcWdNQkz9oquX3vU5I8gKA-KWuzVPkaQbopk/edit?usp=sharing)
* Presenter code: [https://gist.github.com/jserranohidalgo/31e7ea8efecfb36276cf](https://gist.github.com/jserranohidalgo/31e7ea8efecfb36276cf)

**Disclaimer**: these are my own notes from the presentation.

**Bonus**: I have ported the ideas to _Swift_ using a _playground_ (_OS X_ and _Xcode_ are required) ([download](Functional_Programming.playground.zip?raw=true))

---

**Note**: [FP.sc](FP.sc) is a Scala Worksheet version of this

We will see functions as objects first:

```
scala> val suma2: (Int, Int) => Int = (x: Int, y: Int) => x+y
suma2: (Int, Int) => Int = <function2>

scala> suma2(4, 3)
res0: Int = 7

scala> val suma1: Int => Int => Int = (x: Int) => (y: Int) => x+y
suma1: Int => (Int => Int) = <function1>
```
That is a function that receives an `Int` and returns a function that receives an `Int` and returns an `Int`.

```
scala> suma1(4)
res1: Int => Int = <function1>
```

That is a _partial function_.

```
scala> res1(5)
res2: Int = 9

scala> suma2.curried
res3: Int => (Int => Int) = <function1>
```

That is a _currified_ version of `suma2`.

```
scala> suma2.curried(4)(5)
res4: Int = 9

scala> suma2.tupled
res5: ((Int, Int)) => Int = <function1>

scala> suma2.tupled((4,5))
res6: Int = 9

scala> lazy val suma2: (Int, Int) => Int = ???
suma2: (Int, Int) => Int = <lazy>
```

Another way of defining the same functions:

```
scala> val suma2: Function2[Int, Int, Int] = (x: Int, y: Int) => x+y
suma2: (Int, Int) => Int = <function2>

scala> val suma1: Function1[Int, Function1[Int, Int]] = x => y => x + y
suma1: Int => (Int => Int) = <function1>
```

What we have done is representing functions using objects.

That has a problem: functions cannot have generics.

Another option is to use methods:

```
scala> def suma2(x: Int, y: Int): Int = x+y
suma2: (x: Int, y: Int)Int
```

You can create an object from it:

```
scala> suma2 _
res5: (Int, Int) => Int = <function2>

scala> def suma1(x: Int)(y: Int): Int = x+y
suma1: (x: Int)(y: Int)Int
```

It has blocks of parameters and you can do partials.

```
scala> suma1(5)
<console>:9: error: missing arguments for method suma1;
follow this method with `_' if you want to treat it as a partially applied function
              suma1(5)
                   ^

scala> suma1(5) _
res9: Int => Int = <function1>

scala> res9(7)
res10: Int = 12

scala> suma1 _
res11: Int => (Int => Int) = <function1>
```

That is a currified function.

_Generics_:

```
scala> def f[T](t: T): T = t
f: [T](t: T)T
```

That is something that we cannot do with objects.

```
scala> f[Int](3)
res12: Int = 3
```

You have to see that as two arguments, `Int` and `3`, that helps.

Very important: **use pure functions**.

```
scala> def suma2(x: Int, y: Int): Int = {
     |   println("INFO: sumando " + x + "+" + y)
     |   x+y
     | }
suma2: (x: Int, y: Int)Int

scala> suma2(3,4)
INFO: sumando 3+4
res13: Int = 7
```

That is not a pure function.
It does not declare everything it does.
Pure functions can only do internal things.

_Impure functions_ are _evil_:
* Side effects make things difficult to understand.
* Testing is also difficult (you need mocks, etc).
* Modularity and reusability are also a problem if there are side effects.
* Efficiency and scalability: pure functions can be executed in any order.

But we cannot only have pure functions because someone has to do the job: database, web services, log...

So how do we purify functions?
Logging has to be part of the result.
It has to be declared in the signature, as a data type.

_Purification_:

```
scala> trait LoggingInstruction
defined trait LoggingInstruction

(traits are something similar to abstract classes)

scala> case class Info(msg: String) extends LoggingInstruction
defined class Info

scala> case class Warn(msg: String) extends LoggingInstruction
defined class Warn

scala> case class Debug(msg: String) extends LoggingInstruction
defined class Debug

scala> Warn("Sumando 3 + 5")
res14: Warn = Warn(Sumando 3 + 5)

scala> Warn("Sumando 3 + 5"): LoggingInstruction
res15: LoggingInstruction = Warn(Sumando 3 + 5)
```

We will return the log trace as a value:

```
scala> def suma2(x: Int, y: Int): (LoggingInstruction, Int)
     | = {
     |   val resultado = x + y
     |   (Info(s"$x + $y = $resultado"), resultado)
     | }
suma2: (x: Int, y: Int)(LoggingInstruction, Int)
```

That is the essence of function purification.

```
scala> suma2(4, 5)
res16: (LoggingInstruction, Int) = (Info(4 + 5 = 9),9)
```

The result is a _tuple_:

```
scala> res16._1
res18: LoggingInstruction = Info(4 + 5 = 9)

scala> res16._2
res19: Int = 9

scala> (4,5): Tuple2[Int,Int]
res20: (Int, Int) = (4,5)
```

To represent that we define a _type alias_:

```
scala> type Logging[T] = (LoggingInstruction, T)
defined type alias Logging
```

This is the same as before, but we are giving it a name.

```
scala> def suma2(x: Int, y: Int): Logging[Int] = ???
suma2: (x: Int, y: Int)Logging[Int]
```

That helps to understand it conceptually.
The function returns an integer but decorated with logging instructions.

```
scala> def suma2(x: Int, y: Int): Logging[Int] =
     |   (Info("ddd"), x+y)
suma2: (x: Int, y: Int)Logging[Int]
```

**In the real world every side effect should have its own type.**

Programs have the _interpreter_ also to be able to deal with the side effects.
We will have the pure part and the impure part as two separate parts.

Testing should be easy then:

```
scala> assert( suma2(4,5)._2 == 9 )

scala> assert( suma2(4,5)._1 == Info("ddd") )
```

What is the gain?
* Most part of the code can be tested, is modular, can be composed (but the other part has to exist also)
* Our logging instructions can be implemented with println or log4j, etc.
* The business logic of my application will remain unaffected by that.
* The instructions set that I designed is not going to change.
* I have designed a language to define my effects.

All this is the _Interpreter_ pattern (_Gang of Four_).

My interpreter could remove the decoration and return the pure value:

```
scala> def runIO(r: Logging[Int]): Int = {
     |   val (log, i) = r
     |   log match {
     |     case Info(msg) => println(s"INFO: $msg")
     |     case _ => println("otra instruccion")
     |   }
     |   i
     | }
runIO: (r: Logging[Int])Int
```

`val (log, i) = r` is called _Extractor_ in Scala.

With side effect:

```
scala> runIO( suma2(4,5) )
INFO: ddd
res23: Int = 9
```

No side effect:

```
scala> suma2(4,5)
res24: Logging[Int] = (Info(ddd),9)
```

E.g. a controller in the _Play_ framework:

```
def controller(x: HTTPRequest): HTTPResult = ...
```

That is not pure because it will have to go to the database, etc.

**Composition**

A functional program is a function composed of multiple functions.

```
scala> def suma2(x: Int, y: Int): Int = x+y
suma2: (x: Int, y: Int)Int

scala> def negado(x: Int): Int = { println("negando..."); -x }
negado: (x: Int)Int

scala> negado( suma2(4,5) )
negando...
res25: Int = -9
```

I can do that because the signatures match.

If I have decorated functions then I cannot match them:

```
scala> def suma2(x: Int, y: Int): Logging[Int] = ???
suma2: (x: Int, y: Int)Logging[Int]

scala> def negado(x: Int): Logging[Int] = ???
negado: (x: Int)Logging[Int]

scala> negado( suma2(4,5) )
<console>:12: error: type mismatch;
 found   : Logging[Int]
    (which expands to)  (LoggingInstruction, Int)
 required: Int
              negado( suma2(4,5) )
```

How can we solve that problem?

We have to define a special compose operator (`bind/flatMap`)

```
scala> case class Multilog(logs: List[LoggingInstruction]) extends LoggingInstruction
defined class Multilog

scala> case class Logging[T](log: LoggingInstruction, result: T) {
     |   def bind[U](f: T => Logging[U]): Logging[U] = {
     |     val Logging(log2, result2) = f(result)
     |     Logging(Multilog(List(log, log2)), result2)
     |   }
     | }
defined class Logging

scala> def negacionPuraConLogging(n: Int): Logging[Int] = Logging(Warn(s"-$n"), -n)
negacionPuraConLogging: (n: Int)Logging[Int]

scala> def sumaPuraConLogging(x: Int, y: Int): Logging[Int] = {
     |   val resultado = x + y
     |   Logging(Info(s"$x + $y = $resultado"), resultado)
     | }
sumaPuraConLogging: (x: Int, y: Int)Logging[Int]

scala> sumaPuraConLogging(3,4) bind negacionPuraConLogging
res27: Logging[Int] = Logging(Multilog(List(Info(3 + 4 = 7), Warn(-7))),-7)
```

Now it works and it contains all the information (result of the operation and composition of the logging effects).

_Map_:

```
scala> case class Logging[T](log: LoggingInstruction, result: T){
     |
     |   def bind[U](f: T => Logging[U]): Logging[U] = {
     |     val Logging(log2, result2) = f(result)
     |     Logging(Multilog(List(log, log2)), result2)
     |   }
     |
     |   def map[U](f: T => U): Logging[U] =
     |     Logging(log, f(result))
     |
     | }
defined class Logging

scala> def sumaPuraConLogging(x: Int, y: Int): Logging[Int] = {
     |   val resultado = x + y
     |   Logging(Info(s"$x + $y = $resultado"), resultado)
     | }
sumaPuraConLogging: (x: Int, y: Int)Logging[Int]

scala> def negacionPura(n: Int): Int = -n
negacionPura: (n: Int)Int

scala> sumaPuraConLogging(3,4) map negacionPura
res28: Logging[Int] = Logging(Info(3 + 4 = 7),-7)
```

There are other composition operators: e.g. `pure`:

`pure` will encapsulate a value in something with effects but the effect is empty.

```
scala> def pure[T](t: T): Logging[T] = Logging(Multilog(List()), t)
pure: [T](t: T)Logging[T]

scala> (negacionPura _) andThen pure
res29: Int => Logging[Int] = <function1>

scala> res29(7)
res33: Logging[Int] = Logging(Multilog(List()),-7)
```

**Monads**

Data types that represent computations of values of some type which can be _concatenated_. This is the `flatMap/bind` combinator.

They also must allow us to create a pure computation from a given value, through the `point/pure` operator.

**is it so painful to do Functional Programming?**

_Scalaz_ library already implements all those operators that we had to create to work with pure functions, functions with effects, etc.