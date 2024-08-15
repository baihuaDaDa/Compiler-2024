grammar Mx;

program
    : definition* EOF
    ;

definition : funcDef | classDef | varDef;

funcDef : returnType Identifier LeftParen (type Identifier (Comma type Identifier)*)? RightParen suite;

classDef : Class Identifier LeftBrace (varDef | classBuild | funcDef)* RightBrace Semi;

classBuild : Identifier LeftParen RightParen suite;

suite : LeftBrace (statement)* RightBrace;

statement
    : varDef #varDefStmt
    | expression Semi #exprStmt
    | If LeftParen condition=expression RightParen thenStmt=statement (Else elseStmt=statement)? #ifStmt
    | For LeftParen init=statement? cond=expression? Semi step=expression? RightParen body=statement #forStmt
    | While LeftParen condition=expression RightParen statement #whileStmt
    | Break Semi #breakStmt
    | Continue Semi #continueStmt
    | Return expression? Semi #returnStmt
    | Semi #emptyStmt
    | suite #suiteStmt
    ;

varDef : type varDefUnit (Comma varDefUnit)* Semi;
varDefUnit : Identifier (Assign expression)?;

expression
    : New baseType (LeftBracket RightBracket)+ constArray #newArrayExpr
    | New baseType (LeftBracket expression? RightBracket)+ #newEmptyArrayExpr
    | New baseType (LeftParen RightParen)? #newTypeExpr
    | expression op=Dot Identifier #memberExpr
    | func=expression LeftParen (expression (Comma expression)*)? RightParen #funcCallExpr
    | array=expression LeftBracket index=expression RightBracket #indexExpr
    | expression op=(Increment | Decrement) #unaryExpr
    | <assoc=right> op=(Minus | Not | LogicNot) expression #unaryExpr
    | <assoc=right> op=(Increment | Decrement) expression #preSelfExpr
    | expression op=(Mul | Div | Mod) expression #binaryExpr
    | expression op=(Plus | Minus) expression #binaryExpr
    | expression op=(Sla | Sra) expression #binaryExpr
    | expression op=(Gt | Lt | Gte | Lte) expression #binaryExpr
    | expression op=(Eq | Neq) expression #binaryExpr
    | expression op=And expression #binaryExpr
    | expression op=Xor expression #binaryExpr
    | expression op=Or expression #binaryExpr
    | expression op=LogicAnd expression #binaryExpr
    | expression op=LogicOr expression #binaryExpr
    | <assoc=right> condition=expression Query thenExpr=expression Colon elseExpr=expression #ternaryExpr
    | <assoc=right> expression op=Assign expression #assignExpr
    | LeftParen expression RightParen #parenExpr
    | Identifier #atomExpr
    | This #atomExpr
    | literal #atomExpr
    | fString #fStringExpr
    ;

returnType : Void | type;
type : baseType (LeftBracket RightBracket)*;
baseType : defaultType | Identifier;
defaultType : Bool | Int | String;

// fString
fString : ((Quote2Dollar expression (Dollar2Dollar expression)*? Dollar2Quote) | Quote2Quote);
Quote2Dollar : 'f"' (Escape | '$$' | ~[\\"\n\t\f\r$])* '$';
Dollar2Dollar : '$' (Escape | '$$' | ~[\\"\n\t\f\r$])* '$';
Dollar2Quote : '$' (Escape | '$$' | ~[\\"\n\t\f\r$])*? '"';
Quote2Quote : 'f"' (Escape | '$$' | ~[\\"\n\t\f\r$])*? '"';

// literal
literal : ConstDecInt | ConstString | logic | constArray | Null;
logic : True | False;
constArray : LeftBrace (literal (Comma literal)*)? RightBrace;

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
ConstDecInt : '0' | [1-9] [0-9]*;
// string
Quote : '"';
Escape : '\\\\' | '\\n' | '\\"';
ConstString : Quote (Escape | ~[\\"\n\f\r\t])*? Quote; // end
/* Null */
