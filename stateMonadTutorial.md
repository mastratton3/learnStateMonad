Practical Application of the State Monad 
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
  If a field is null, it should be replaced with the prior value as long as no more than 10s has past since the last real reading 









[1]: https://www.amazon.com/Functional-Programming-Scala-Paul-Chiusano/dp/1617290653