grammar Mx;

program
    : (funcDef | classDef | varDef)* EOF
    ;

funcDef : (type | Void) Identifier LeftParen (type Identifier (Comma type Identifier)*)? RightParen suite;

classDef : Class Identifier LeftBrace (varDef | classBuild | funcDef)* RightBrace Semi;

classBuild : Identifier LeftParen RightParen suite;

suite : LeftBrace (statement)* RightBrace;

statement
    : varDef
    | expression Semi
    | If LeftParen expression RightParen (statement | suite) (Else (statement | suite))?
    | For LeftParen (varDef | expression)? Semi expression? Semi expression? RightParen (statement | suite)
    | While LeftParen expression RightParen (statement | suite)
    | Break Semi
    | Continue Semi
    | Return expression? Semi
    | Semi
    | suite
    ;

varDef : type Identifier (Assign expression)? (Comma Identifier (Assign expression)?)* Semi;

expression
    : New baseType (LeftBracket RightBracket)+ array #newExpr
    | New baseType ((LeftBracket DecInt RightBracket)* (LeftBracket RightBracket)*) #newExpr
    | New baseType LeftParen RightParen #newExpr
    | expression op=Dot Identifier #memberExpr
    | expression LeftParen (expression (Comma expression)*)? RightParen #funcCallExpr
    | expression LeftBracket expression RightBracket #indexExpr
    | expression op=(Increment | Decrement) #sucSelfExpr
    | <assoc=right> op=(Minus | Not | LogicNot) expression #unaryExpr
    | <assoc=right> op=(Increment | Decrement) expression #preSelfExpr
    | expression op=(Mul | Div | Mod) expression #binExpr
    | expression op=(Plus | Minus) expression #binExpr
    | expression op=(Sla | Sra) expression #binExpr
    | expression op=(Gt | Lt | Gte | Lte) expression #binExpr
    | expression op=(Eq | Neq) expression #binExpr
    | expression op=And expression #binExpr
    | expression op=Xor expression #binExpr
    | expression op=Or expression #binExpr
    | expression op=LogicAnd expression #binExpr
    | expression op=LogicOr expression #binExpr
    | <assoc=right> expression Query expression Colon expression #ternaryExpr
    | <assoc=right> expression op=Assign expression #assignExpr
    | LeftParen expression RightParen #parenExpr
    | Identifier #atomExpr
    | This #atomExpr
    | literal #atomExpr
    | fString #fStrExpr
    ;

type : baseType (LeftBracket RightBracket)*;
baseType : defaultType | Identifier;
defaultType : Bool | Int | String;

// fString
fString : (Quote2Dollar expression (Dollar2Dollar expression)*? Dollar2Quote) | Quote2Quote;
Quote2Dollar : 'f"' (Escape | '$$' | ~[\\"\n\t\f\r$])* '$';
Dollar2Dollar : '$' (Escape | '$$' | ~[\\"\n\t\f\r$])* '$';
Dollar2Quote : '$' (Escape | '$$' | ~[\\"\n\t\f\r$])*? '"';
Quote2Quote : 'f"' (Escape | '$$' | ~[\\"\n\t\f\r$])*? '"';

// literal
literal : DecInt | Str | logic | array | Null;
logic : True | False;
array : LeftBrace (literal (Comma literal)*)? RightBrace;

// reserved words
Void : 'void';
Bool : 'bool';
Int : 'int';
String : 'string';
New : 'new';
Class : 'class';
Null : 'null';
True : 'true';
False : 'false';
This : 'this';
If : 'if';
Else : 'else';
For : 'for';
While : 'while';
Break : 'break';
Continue : 'continue';
Return : 'return';

// characters
Plus : '+';
Minus : '-';
Mul : '*';
Div : '/';
Mod : '%';
Gt : '>';
Lt : '<';
Gte : '>=';
Lte : '<=';
Neq : '!=';
Eq : '==';
LogicAnd : '&&';
LogicOr : '||';
LogicNot : '!';
Sla : '<<';
Sra : '>>';
And : '&';
Or : '|';
Xor : '^';
Not : '~';
Assign : '=';
Increment : '++';
Decrement : '--';
Dot : '.';
LeftBracket : '[';
RightBracket : ']';
LeftParen : '(';
RightParen : ')';
Query : '?';
Colon : ':';
Semi : ';';
Comma : ',';
LeftBrace : '{';
RightBrace : '}';

// whitespace
Whitespace : [ \t\r\n]+ -> skip;

// comments
LineComment : '//' ~[\r\n]* -> skip;
BlockComment : '/*' .*? '*/' -> skip;

// identifiers
Identifier : [a-zA-Z_] [a-zA-Z0-9_]*;

// literals
DecInt : '0' | [1-9] [0-9]*;
// string
Quote : '"';
Escape : '\\\\' | '\\n' | '\\"';
Str : Quote (Escape | ~[\\"\n\f\r\t])*? Quote; // end
/* Null */
