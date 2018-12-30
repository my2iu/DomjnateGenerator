/** 
 * Taken from the Typescript 1.8 documentation 
 *   https://github.com/Microsoft/TypeScript/blob/master/doc/spec.md
 * 
 * Plus, some bits were added by checking out the new Typescript features at
 *   https://www.typescriptlang.org/docs/handbook/advanced-types.html 
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
   | 'extends' KeyOf type   // index type query operator
	;

KeyOf: 'keyof';

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
   (propertySignatureReadOnly)? propertyName optional? (typeAnnotation)?
   ;
   
propertySignatureReadOnly: 'readonly' ;
optional: '?' ;   

propertyName:
   identifierName
   | StringLiteral
   | NumericLiteral
	;

typeAnnotation:
   ':' (type | booleanTypeGuard)
   ;
   
booleanTypeGuard:
	bindingIdentifier 'is' type
	;

type:
   unionOrIntersectionOrPrimaryType
   | functionType
   | constructorType
   | conditionalType
	;

notOptional: '-?' ;   

indexSignature:
	(propertySignatureReadOnly)?  '[' bindingIdentifier 'in' (KeyOf)? type ']' (notOptional|optional)? typeAnnotation   # indexSignatureMapped // mapped type
   | (propertySignatureReadOnly)? '[' bindingIdentifier ':' 'string' ']' typeAnnotation # indexSignatureString
   | (propertySignatureReadOnly)? '[' bindingIdentifier ':' 'number' ']' typeAnnotation # indexSignatureNumber
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
   | primaryType /*[no LineTerminator here]*/ primaryTypeIndexable
   | tupleType
   | typeQuery
   | thisType
   	| inferredType    // Only appears in conditional types
	| StringLiteral   // No idea what's going on there, but this does show up
	| NumericLiteral   // No idea what's going on there, but this does show up
	;

primaryTypeIndexable:
	'[' ']'  // arrayType
	| '[' typeReference ']' // New rule by me for indexed access operator
	| '[' KeyOf typeReference ']' // New rule by me for indexed access operator or map types
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
   (accessibilityModifier)? bindingIdentifierOrPattern optional (typeAnnotation)?
   | (accessibilityModifier)? bindingIdentifierOrPattern (typeAnnotation)? initializer
   | bindingIdentifier optional ':' StringLiteral
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

methodSignature:
	propertyName (optional)? callSignature;	

constructSignature:
   'new' (typeParameters)? '(' (parameterList)? ')' (typeAnnotation)?
   ;


functionType:
   (typeParameters)? '(' (parameterList)? ')' '=>' (type | booleanTypeGuard)
   ;

constructorType:
   'new' (typeParameters)? '(' (parameterList)? ')' '=>' type
   ;

conditionalType:
	bindingIdentifier 'extends' type '?' type ':' type 
	;

thisType:
   'this' ;

inferredType:
	'infer' type;

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
   | 'declare' ambientNamespaceDeclaration
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

ambientNamespaceDeclaration:
   'namespace' identifierPath '{' ambientNamespaceBody '}'
   ;

ambientNamespaceBody:
   (ambientNamespaceElements)?
   ;

ambientNamespaceElements:
   (ambientNamespaceElement)+
//   | ambientNamespaceElements ambientNamespaceElement
   ;

ambientNamespaceElement:
   ('export')? ambientVariableDeclaration
//   | ('export')? ambientLexicalDeclaration
   | ('export')? ambientFunctionDeclaration
//   | ('export')? ambientClassDeclaration
   | ('export')? interfaceDeclaration
//   | ('export')? ambientEnumDeclaration
   | ('export')? ambientNamespaceDeclaration
//   | ('export')? importAliasDeclaration
   ;

identifierPath:
   bindingIdentifier
   | identifierPath '.' bindingIdentifier
	;

WS: [ \t\r\n]+ -> skip ;

COMMENT: ('/*' .*? '*/'
		| '//' ~[\r\n]*)
		 -> channel(HIDDEN);
