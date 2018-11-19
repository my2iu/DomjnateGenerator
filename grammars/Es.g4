// Parts of the ECMAScript grammar
// Taken from http://www.ecma-international.org/ecma-262/6.0/
grammar Es;


bindingPattern:
	objectBindingPattern
	| arrayBindingPattern
	;
	
objectBindingPattern:
	'{' '}'
	| '{' bindingPropertyList '}'
	| '{' bindingPropertyList ',' '}'
	;
	
arrayBindingPattern:
	'[' (elision)? (bindingRestElement)? ']'
	| '[' bindingElementList ']'
	| '[' bindingElementList ',' (elision)? (bindingRestElement)? ']'
	;

bindingPropertyList:
	bindingProperty
	| bindingPropertyList ',' bindingProperty
	;

bindingElementList:
	bindingElisionElement
	| bindingElementList ',' bindingElisionElement
	;

bindingElisionElement:
	(elision)? bindingElement
	;
	
bindingProperty:
	singleNameBinding ;
propertyName: bindingElement ;
bindingElement:
	singleNameBinding
	| bindingPattern (initializer)?
	;
singleNameBinding:
	bindingIdentifier (initializer)?
	;
bindingRestElement:
	'...' bindingIdentifier ;

initializer:
	'=' // assignmentExpression    // I don't think initializers will occur in declaration files
	; 

elision:
	','
	| elision ','
	;

bindingIdentifier: identifier ;
identifierReference: identifier ;

identifier: identifierName ;

// Skips encoded Unicode characters
identifierName:
	'extends' 
	| 'public'
	| 'type'
	| 'this'
	| 'string'
	| 'declare'
	| 'readonly'
	| IdentifierNameLex
	;

IdentifierNameLex:
	[_$\p{ID_Start}][_$\p{ID_Continue}\u200c\u200d]*;

// Skips encoded Unicode characters or multi-line strings
StringLiteral:  
	'"' (~[\\\r\n"] | [\\]['"bfnrtv])* '"'
	| '\'' (~[\\\r\n'] | [\\]['"bfnrtv])* '\''
	;

NumericLiteral:
	DecimalLiteral
	| BinaryIntegerLiteral
	| OctalIntegerLiteral
	| HexIntegerLiteral
	;

DecimalLiteral:
	DecimalIntegerLiteral '.' (DecimalDigits)? (ExponentPart)?
	| '.' DecimalDigits (ExponentPart)?
	| DecimalIntegerLiteral (ExponentPart)?
	;

DecimalIntegerLiteral:
	'0'
	| NonZeroDigit (DecimalDigits)?
	;

DecimalDigits:
	[0-9]+
	;


NonZeroDigit:
	[1-9] ;

ExponentPart:
	ExponentIndicator SignedInteger
	;
	
ExponentIndicator:
	[eE] ;

SignedInteger:
	DecimalDigits
	| '+' DecimalDigits
	| '-' DecimalDigits
	;

BinaryIntegerLiteral:
	'0b' BinaryDigits
	| '0B' BinaryDigits
	;

BinaryDigits:
	[01]+ ;
	
OctalIntegerLiteral:
	'0o' OctalDigits
	| '0O' OctalDigits
	;
	
OctalDigits:
	[0-7]+ ;
	

HexIntegerLiteral:
	'0x' HexDigits
	| '0X' HexDigits
	;
	
HexDigits:
	[0-9a-fA-F]+ ;
	
