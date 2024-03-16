Narrative:
Our lisp interpreter should be able to 
perform those lisp operations. 

Scenario: built-in lisp operators

Given a lisp interpreter

When the expression entered is <expression>
Then the result should be <result>

Examples:

|expression  |result|

|(quote (+ 1 2))|(+ 1 2)|
|(if #t 3 4)|3|
|(if #f 3 4)|4|
|(if (> 10 20) (+ 1 1) (+ 3 3))|6|
|(if (> 1 2) 3 5)|5|
|(cons 1 2)|(1 . 2)|
|(cons 1 ())|(1)|
|(cons () 1)|(() . 1)|
|(cons () ())|(())|
|(cons (cons 1 2)  3)|((1 . 2) . 3)|
|(cons 1 (cons 2 ())) |(1 2)|
|(cons 1 (cons 2 (cons 3 ()))) |(1 2 3)|
|(list 1 2 3)|(1 2 3)|
|(list 1 (list 2 3) (list 4 5))|(1 (2 3) (4 5))|
|(cons 1 (list 2 3))|(1 2 3)|
|(car (list 1 2 3))|1|
|(cdr (list 1 2 3))|(2 3)|
|(car (cons 1 2))| 1  |
|(cdr (cons 1 2))| 2  |
|(car (cons 1 (cons 2 ()))) | 1 |
|(cdr (cons 1 (cons 2 ()))) | (2) |
|(cons (+ 1 2) (* 2 3))|(3 . 6)|
|(cons (+ 1 2) (list 2 1))|(3 2 1)|
|(cons (list 1 2) (+ 2 1))|((1 2) . 3)|
|(cons 1 (cons 2 nil)) |(1 2)|
|(list (+ 1 2) 4 5)|(3 4 5)|

Scenario: additional tests from V. Valembois

Given a lisp interpreter

When the expression entered is <expression>
Then the result should be <result>

Examples:

|expression  |result|

|(= 2 2 2)|#t|
|(= 2)|#t|
|(< 1)|#t|
|(> 1)|#t|
|(< 1 2 3)|#t|
|(> 1 2 3)|#f|

Scenario: simple definitions

Given a lisp interpreter

When the expression entered is (define a 5)
Then the result should be 5

When the expression entered is (define b 10)
Then the result should be 10

When the expression entered is <expression>
Then the result should be <result>

Examples:

|expression  |result|
|(+ a b)       | 15   |
|(- a b)     | -5   |
|(* a b)     |  50   |
|(* 1 a 2 b) | 100   |
|(/ b a)     |  2   |
|(if (> a b) a b)   |  10   |
|(if (< a b) a b)   |  5   |
|(cons a (cons b ()))| (5 10) |

Scenario: more complex functions (lambdas)

Given a lisp interpreter

When the expression entered is (define twice (lambda (x) (* 2 x)))
Then the result should be lambda (x) (* 2 x)

When the expression entered is (define strange (lambda (f x) (f (f x))))
Then the result should be lambda (f x) (f (f x))

When the expression entered is (strange twice 10)
Then the result should be 40

Scenario: recursive functions - factorial

Scenario: recursive functions - fibonacci

Scenario: back to LCPF

Scenario: some potential error

Scenario: some students are picky about testing
