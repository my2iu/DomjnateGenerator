// Parts of the ECMAScript grammar
// Taken from http://www.ecma-international.org/ecma-262/6.0/
grammar Es;

bindingIdentifier : identifier ;

identifier : IdentifierName ;

// Don't bother with full Unicode support for now.
IdentifierName : [A-Za-z_$][A-Za-z_$0-9]*;
