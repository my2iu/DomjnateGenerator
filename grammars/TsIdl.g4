/** Taken from the Typescript 1.8 documentation 
 *  https://github.com/Microsoft/TypeScript/blob/master/doc/spec.md
 */
grammar TsIdl;

import Es;

declarationSourceFileEOF:
	declarationSourceFile EOF ;

declarationSourceFile:
	declarationScript
	| declarationModule;

declarationModule: ;  // unimplemented

declarationScript:
	declarationScriptElements
	|
	;

declarationScriptElements:
	declarationScriptElement
	| declarationScriptElements declarationScriptElement
	;

declarationScriptElement:
	declarationElement
//	| ambientModuleDeclaration
	;

declarationElement:
	interfaceDeclaration
	| typeAliasDeclaration
//	| namespaceDeclaration
	| ambientDeclaration
//	| importAliasDeclaration
	;


typeAliasDeclaration:
   ('declare')? 'type' bindingIdentifier (typeParameters)? '=' type ';' ;

interfaceDeclaration:
   'interface' bindingIdentifier (typeParameters)? (interfaceExtendsClause)? objectType
   ;

interfaceExtendsClause:
   'extends' classOrInterfaceTypeList
   ;

classOrInterfaceTypeList:
   classOrInterfaceType
   | classOrInterfaceTypeList ',' classOrInterfaceType
   ;

classOrInterfaceType:
	typeReference
	;

typeReference:
   typeName /*[no LineTerminator here]*/ (typeArguments)?
	;

typeName:
   identifierReference
   | namespaceName '.' identifierReference
   ;

namespaceName:
   identifierReference
   | namespaceName '.' identifierReference
   ;

typeArguments:
   '<' typeArgumentList '>'
	;

typeArgumentList:
   typeArgument
   | typeArgumentList ',' typeArgument
	;

typeArgument:
   type
	;

typeParameters:
	'<' typeParameterList '>'
	;

typeParameterList:
	typeParameter
	| typeParameterList ',' typeParameter
	;

typeParameter:
   bindingIdentifier (constraint)? (typeParameterDefault)?
	;

constraint:
   'extends' type
   | 'extends' 'keyof' type   // index type query operator
	;

typeParameterDefault:  // New rule for type parameter defaults
	'=' type ;

objectType:
   '{' (typeBody)? '}'
	;

typeBody:
   typeMemberList (';')?
   | typeMemberList (',')?
	;

typeMemberList:
   typeMember
   | typeMemberList ';' typeMember
   | typeMemberList ',' typeMember
   ;

typeMember:
   propertySignature
   | callSignature
   | constructSignature
   | indexSignature
   | methodSignature
   ;

propertySignature:
   (propertySignatureReadOnly)? propertyName (propertySignatureOptional)? (typeAnnotation)?
   ;
   
propertySignatureReadOnly : 'readonly' ;
propertySignatureOptional : '?' ;   

propertyName:
   identifierName
   | StringLiteral
   | NumericLiteral
	;

typeAnnotation:
   ':' type
   ;

type:
   unionOrIntersectionOrPrimaryType
   | functionType
   | constructorType
	;

indexSignature:
   '[' bindingIdentifier ':' 'string' ']' typeAnnotation
   | '[' bindingIdentifier ':' 'number' ']' typeAnnotation
   ;

unionOrIntersectionOrPrimaryType:
	unionOrIntersectionOrPrimaryType '|' intersectionOrPrimaryType
   | intersectionOrPrimaryType
	;

intersectionOrPrimaryType:
   intersectionOrPrimaryType '&' primaryType
   | primaryType
	;

primaryType:
   parenthesizedType
   | predefinedType
   | typeReference
   | objectType
   | primaryType /*[no LineTerminator here]*/ '[' ']'  // arrayType
   | tupleType
   | primaryType '[' typeReference ']'  // New rule by me for indexed access operator
   | typeQuery
   | thisType
	| StringLiteral   // No idea what's going on there, but this does show up
	| NumericLiteral   // No idea what's going on there, but this does show up
	;

parenthesizedType:
   '(' type ')'
	;

predefinedType:
   'any'
   | 'number'
   | 'boolean'
   | 'string'
   | 'symbol'
   | 'void'
	;
	
callSignature:
	(typeParameters)? '(' (parameterList)? ')' (typeAnnotation)?
	;	

parameterList:
   requiredParameterList
   | optionalParameterList
   | restParameter
   | requiredParameterList ',' optionalParameterList
   | requiredParameterList ',' restParameter
   | optionalParameterList ',' restParameter
   | requiredParameterList ',' optionalParameterList ',' restParameter
   ;

requiredParameterList:
   requiredParameter
   |requiredParameterList ',' requiredParameter
   ;

requiredParameter:
   (accessibilityModifier)? bindingIdentifierOrPattern (typeAnnotation)?
   | bindingIdentifier ':' StringLiteral
   ;

accessibilityModifier:
   'public'
   | 'private'
   | 'protected'
   ;

bindingIdentifierOrPattern:
   bindingIdentifier
   | bindingPattern
   ;

optionalParameterList:
   optionalParameter
   | optionalParameterList ',' optionalParameter
   ;

optionalParameter:
   (accessibilityModifier)? bindingIdentifierOrPattern '?' (typeAnnotation)?
   | (accessibilityModifier)? bindingIdentifierOrPattern (typeAnnotation)? initializer
   | bindingIdentifier '?' ':' StringLiteral
   ;

restParameter:
   '...' bindingIdentifier (typeAnnotation)?
   ;



tupleType:
   '[' tupleElementTypes ']'
	;

tupleElementTypes:
   tupleElementType
   | tupleElementTypes ',' tupleElementType
   ;

tupleElementType:
   type
   ;	

methodSignature :
	propertyName ('?')? callSignature;	

constructSignature:
   'new' (typeParameters)? '(' (parameterList)? ')' (typeAnnotation)?
   ;


functionType:
   (typeParameters)? '(' (parameterList)? ')' '=>' type
   ;

constructorType:
   'new' (typeParameters)? '(' (parameterList)? ')' '=>' type
   ;

thisType:
   'this' ;

typeQuery:
   'typeof' typeQueryExpression ;

typeQueryExpression:
   identifierReference
   | typeQueryExpression '.' identifierName
   ;

	
/*
Mutually left-recursive
arrayType:
   primaryType  '[' ']'
	;

unionType:
	unionOrIntersectionOrPrimaryType '|' intersectionOrPrimaryType
	;

intersectionType:
   intersectionOrPrimaryType '&' primaryType
	;
*/	

ambientDeclaration:
   'declare' ambientVariableDeclaration
   | 'declare' ambientFunctionDeclaration
//   | 'declare' ambientClassDeclaration
//   | 'declare' ambientEnumDeclaration
//   | 'declare' ambientNamespaceDeclaration
   ;
   
ambientVariableDeclaration:
   'var' ambientBindingList ';'
   | 'let' ambientBindingList ';'
   | 'const' ambientBindingList ';'
   ;

ambientBindingList:
   ambientBinding
   | ambientBindingList ',' ambientBinding
   ;

ambientBinding:
   bindingIdentifier (typeAnnotation)?
   ;
   
ambientFunctionDeclaration:
	'function' bindingIdentifier callSignature ';'
	;   

WS : [ \t\r\n]+ -> skip ;

COMMENT : ('/*' .*? '*/'
		| '//' ~[\r\n]*)
		 -> channel(HIDDEN);
