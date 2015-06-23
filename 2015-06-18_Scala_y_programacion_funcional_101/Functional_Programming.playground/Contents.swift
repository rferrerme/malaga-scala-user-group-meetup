/*:
# Functional Programming in Swift

### Based on the Malaga Scala User Group meetup on 2015-06-08:

* [Slides](https://docs.google.com/presentation/d/1JYhcSe3JcWdNQkz9oquX3vU5I8gKA-KWuzVPkaQbopk/edit?usp=sharing)
* [Code](https://gist.github.com/jserranohidalgo/31e7ea8efecfb36276cf)

Pure function:
*/

let sumPure1 = { (x: Int, y: Int) in x+y }
sumPure1(4, 5)

//: Function with side effect:

let sumPureWithLogging1 = { (x: Int, y: Int) -> Int in
    println("I'm a side effect")
    return x+y
}
sumPureWithLogging1(4, 5)

/*:
That is not a pure function:
* It does not declare everything it does
* Pure functions can only do internal things

Impure functions are _evil_:
* Side effects make things difficult to understand
* Testing is also difficult (you need mocks, etc)
* Modularity and reusability are also a problem if there are side effects
* Efficiency and scalability: pure functions can be executed in any order

But we cannot only have pure functions because someone has to do the job: database, web services, log, etc.

How do we purify functions?
* E.g. logging has to be part of the result
* It has to be declared in the signature, as a new data type

New type to take care of the logging information:
*/

enum LoggingInstruction1 {
    case Info(msg: String)
    case Warn(msg: String)
    case Debug(msg: String)

    var description: String {
        switch self {
        case let .Info(msg: msg):
            return "Info(\(msg))"
        case let .Warn(msg: msg):
            return "Warn(\(msg))"
        case let .Debug(msg: msg):
            return "Debug(\(msg))"
        }
    }
}

//: New type to combine logging information and value:

struct Logging1<T> {
    var log: LoggingInstruction1
    var result: T
    
    var description: String {
        return "(\(log.description), \(result)"
    }
}

//: New implementations without side effects:

let sumPureWithLogging2 = { (x: Int, y: Int) -> Logging1<Int> in
    return Logging1<Int>(log: LoggingInstruction1.Info(msg: "\(x) + \(y)"), result: x+y)
}

let operation = sumPureWithLogging2(4, 5)

operation.description

let negPureWithLogging2 = { (x: Int) -> Logging1<Int> in
    return Logging1<Int>(log: LoggingInstruction1.Warn(msg: "Neg \(x)"), result: -x)
}
negPureWithLogging2(7).description

//: Simple test (asserts are not nice in Playgrounds):

operation.log.description == "Info(4 + 5)"
operation.result == 9

/*:
That helps to understand it conceptually.

The function returns an integer but decorated with logging instructions.

In practice every side effect should have its own type.

We will have two separare parts: pure and impure.

Programs have the interpreter also to be able to deal with the side effects.


What is the gain?
* Most part of the code can be tested, it is modular, it can be composed
* But the other part has to exist also...
* Our logging instructions can be implemented with println or log4j, etc
* The business logic of my application will remain unaffected by that
* The instructions set that I designed is not going to change
* I have designed a language to define my effects

My interpreter will remove the decoration and return the pure value:
*/

func runIO(logging: Logging1<Int>) -> Int {
    switch logging.log {
        case let LoggingInstruction1.Info(msg): println("INFO: " + msg)
        case let LoggingInstruction1.Warn(msg): println("WARN: " + msg)
        case let LoggingInstruction1.Debug(msg): println("DEBUG: " + msg)
    }
    return logging.result
}

//: See above (Quicklook of each `println`) for the side effects:

runIO(sumPureWithLogging2(4, 5))
runIO(sumPureWithLogging2(2, 3))
runIO(negPureWithLogging2(8))

/*:
But now it is not possible to do composition because types do not match.

This will not compile:

    negPureWithLogging2(sumPureWithLogging2(4, 5))

We will have to define a special compose operator to solve the problem (`bind`/`flatMap`).

LoggingInstruction needs to be able to combine multiple logs:
*/

enum LoggingInstruction2 {
    case Info(msg: String)
    case Warn(msg: String)
    case Debug(msg: String)
    // Added to be able to do composition
    case MultiLog(logs: [LoggingInstruction2])
    
    var description: String {
        switch self {
        case let .Info(msg: msg):
            return "Info(\(msg))"
        case let .Warn(msg: msg):
            return "Warn(\(msg))"
        case let .Debug(msg: msg):
            return "Debug(\(msg))"
        case let .MultiLog(logs: logs):
            return "Multi(\(logs.map({ $0.description })))"
        }
    }
}

LoggingInstruction2.MultiLog(logs: [LoggingInstruction2.Info(msg: "info1"), LoggingInstruction2.Warn(msg: "warn1")]).description

//: Implementation of the `bind`/`flatMap` operator:

struct Logging2<T> {
    var log: LoggingInstruction2
    var result: T
    
    func bind<U>(f: T -> Logging2<U>) -> Logging2<U> {
        let logging = f(result)
        return Logging2<U>(log: LoggingInstruction2.MultiLog(logs: [log, logging.log]), result: logging.result)
    }
    
    var description: String {
        return "(\(log.description), \(result)"
    }
}

let sumPureWithLogging3 = { (x: Int, y: Int) -> Logging2<Int> in
    return Logging2<Int>(log: LoggingInstruction2.Info(msg: "\(x) + \(y)"), result: x+y)
}

let negPureWithLogging3 = { (x: Int) -> Logging2<Int> in
    return Logging2<Int>(log: LoggingInstruction2.Warn(msg: "Neg \(x)"), result: -x)
}

//: Now we can do composition and it will contain all the information (result of the operation and composition of the logging effects):

sumPureWithLogging3(4, 5).bind(negPureWithLogging3).description

/*:
There are other composition operators:
* E.g. `pure` will encapsulate a value into something with effects, but the effect will be empty:
*/

func pure<T>(t: T) -> Logging2<T> {
    return Logging2<T>(log: LoggingInstruction2.MultiLog(logs: []), result: t)
}

pure(10).description
