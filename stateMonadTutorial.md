Practical Application of the State Monad - Time Series Pre-Processing 
======================================== 

The motivation for writing this blog post came after reading the book [Functional Programming in Scala][1] along with numerous other posts discussing Monads. 

While conceptually all these made sense (most of the time) and I understood what was going on in the book example of a random number generator.
I still felt a bit lost as to how to extend the concept into my daily work. 

Thus, I decided to try and develop a quick example that met the following criteria: 

  * Non-Trivial - Needed to be more advanced than maintaining a row count 
  * Solved a problem encountered in my daily work 
  * Not-overly complex - Needs to be understandable to someone who doesn't have a full FP background 

This led me to a common problem I encountered on a previous project which was: 

How can you fill forward a list of times series measurements when some may be null (Or None in our case) 

And example of this would be a data set which looks like: 
``` 
times(s)  |   value1 
  0       |     1.0 
  1       |     null 
  2       |     2.0 
  3       |     null 
  .... 
  14      |     15.0 
``` 

In this case we have the following requirement: 
  If a field is null, it should be replaced with the prior value as long as no more than 10s has past since the last real reading. 
  
If you may be wondering, what would be the real world application of this? 
  
One potential case could be sensor data that is being recorded by a legacy system that records out of bounds measurements as null. 
This could be caused by any sort of issues such as someone wiggling the cable between the sensor and recorder.  
 
For some basic bookkeeping lets assume that the data is already parsed from the outside source and the value column is already wrapped as Options. 
Below I establish the initial case classes we need: 

``` 
import scalaz._
import Scalaz._
// Note: I typically would delcare these in reverse order, but wanted to be copy paste friendly 
case class Row(time: Int, value: Option[Double]) 
case class RawRow(time: Int, value: Option[Double]) {
  def toRow: Row = Row(time, value)
}
``` 

So in the above case you would expect it to return a result such as: 
```
times(s)  |   value1 
  0       |     1.0 
  1       |     1.0 
  2       |     2.0 
  3       |     2.0 
  .... 
  12      |     2.0 
  13      |     None
  14      |     15.0 
``` 
Side Note: For this you're going to need to use the scalaz library. I would reccomend cloning this repo and using the sbt console command to load a REPL with Scalaz on the classpath. 

Historically, I would probably have writen function that uses internal mutable state but remains pure to the outside caller such as: 

```
def processRawRow(ls: List[RawRow], threshold: Int = 10): List[Row] = {
  var secondsBetween = 0
  var priorMeasurement: Option[Double] = None // Explicit typing required since Scala won't be able to infer this 
  ls.map{
    elem => 
    elem match {
      case RawRow(_, Some(x)) => {
        secondsBetween = 0
        priorMeasurement = elem.value
        elem.toRow
      }
      case RawRow(t, None) if (secondsBetween + 1 < threshold) => {
        secondsBetween += 1
        elem.copy(value = priorMeasurement).toRow
      }
      case _ => {
        secondsBetween += 1
        elem.toRow
      }
    }
  }
}

``` 

Whats the problem with this? Well nothing in particular, but I find it a bit tough to reason about, it isn't as DRY as possible, and tough to understand at a glance. 
While I could potentially clean this up a bit, I think it'd only be incremental improvements. 
 
So what is a potential solution to this problem? 

Well, we can do a state traversal with a State Monad to pass the state information from element to element. 

So what are the types and how does this work? In the general sense, the State Monad accepts and initial state and return the next state along with the result. 

When you combine this with a traversal, the actual passing of the state is abstracted away and is handled automatically. The code this boils down to is below: 
 
``` 
case class PrevState(timeBetween: Int, prevValue: Option[Double])

def procLines(i: RawRow, timeLimit: Int): State[PrevState,Row] = State {
  x: PrevState =>
  val newTime = x.timeBetween + 1

  i match {
    case RawRow(_, Some(y)) => (PrevState(0, Some(y)), i.toRow)
    case RawRow(_, None) if newTime < timeLimit => (PrevState(newTime,x.prevValue),i.copy(value = x.prevValue).toRow)
    case _ => (PrevState(newTime, x.prevValue), i.toRow)
  }
} 
``` 

Now that we have the function that takes the RawRow object(+ timeLimit) and returns a State Monad we can use it to perform a state traversal which will pass the State as it goes through the list. 

What is the signature of this traversal function? Well according to the Scala Docs it is: 
``` 
def traverseS[S, A, B](fa: F[A])(f: (A) ⇒ State[S, B]): State[S, F[B]]
``` 

In our case the generics evaluate out to: 
```
traverseS(fa: List[RawRow])(f: (RawRow) => State[PrevState, Row]): State[PrevState, List[Row]]
``` 

The F in the original just stands for Functor and a List is a functor. Now the only problem we have left is that our original function is of type: 
``` 
(RawRow, Int) => State[PrevState,Row]
``` 
This however can be easily fixed by partially applying the timeLimit argument when we call the traverse function. 

To make this easier to follow along, I will provide a copy and paste version of the test data below: 
```
val testValues = List(
Some(1.0),
None,
Some(2.0),
None,
None,
None,
None,
None,
None,
None,
None,
None,
None,
Some(15.0)
)
val testList = (1 to 14).toList.zip(testValues).map(x => RawRow(x._1,x._2))
```

And now for the actual code that performs the state traversal! 
``` 
val result = testList traverseS(procLines(_, 10)) run (PrevState(0,None: Option[Double]))
``` 

And voila! A fully functional way to traverse a list while maintaining state. We can now get our result out with the following piece of code: 
``` 
result._2
```
If you call the first argumment to that than you will get the last PrevState. If you want a challange than try to think of a way you can return all the intermediate states as well. 

As you can see, the result is the list we expected. 

While this introduction didn't go into the actual types and theory of monads as much as other tutorials do, I hope it helps to show that some of the more advanced features of funcation programming are approachable as well. My goal here is to provide an example that will help other begin using these features. 

In the next edition, we'll dive into how these can be composed together to yield even more powerful operations and further reduce impure code. 

Additionally - There is another file in the repo to provide another example. 


[1]: https://www.amazon.com/Functional-Programming-Scala-Paul-Chiusano/dp/1617290653