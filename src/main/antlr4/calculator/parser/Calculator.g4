grammar Calculator;

start: statement* EOF;

statement
    : declaration ';'
    | printStatement ';'
    | expr ';'
    | block
    ;

declaration
    : 'let' IDENTIFIER '=' expr
    ;

printStatement
    : PRINT '(' expr (COMMA expr)* ')' // Print statement
    ;

expr: additionExpr;

/*  Collect the left-hand side of this PLUS or MINUS as initial using '=', and
    the (op, rest) pairs as lists using '+='.

    See https://github.com/antlr/antlr4/blob/dev/doc/parser-rules.md#rule-element-labels  */
additionExpr
    : initial=multiplicationExpr (op+=(PLUS | MINUS) rest+=multiplicationExpr)*;

multiplicationExpr
    : initial=exponentExpr (op+=(STAR | SLASH) rest+=exponentExpr)* ;

exponentExpr
    : lhs=atom (CARET rhs=exponentExpr)? ;

atom
    : NUMBER #NumberConstant
    | IDENTIFIER #VariableReference
    | '(' expr ')' #GroupingOperator
    | vectorLiteral #VectorConstructor
    ;

vectorLiteral
    : '<' (expr (COMMA expr)*)? '>'
    ;

block: '{' statement* '}';

// Lexer rules
fragment DIGIT: [0-9];

LEFT_PAREN: '(';
RIGHT_PAREN: ')';

CARET: '^';
STAR: '*';
SLASH: '/';
PLUS: '+';
MINUS: '-';

VECTOR_OPEN: '<';
VECTOR_CLOSE: '>';
COMMA: ',';
EQUAL: '=';

PRINT: 'print';

NUMBER: ('-')? DIGIT+ ('.' DIGIT+)?; // Allows integers and decimals

IDENTIFIER: [a-zA-Z_] [a-zA-Z_0-9]*; // Identifiers for vectors (e.g., a, b, m)

SPACES: [ \u000B\t\r\n\p{White_Space}] -> skip; // Skips whitespace
COMMENT: '/*' .*? '*/' -> skip; // Skips block comments
LINE_COMMENT: '//' ~[\r\n]* -> skip; // Skips line comments
