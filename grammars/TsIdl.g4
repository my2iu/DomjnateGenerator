/** Taken from the Typescript 1.8 documentation 
 *  https://github.com/Microsoft/TypeScript/blob/master/doc/spec.md
 */
grammar TsIdl;

import Es;

declarationSourceFile:
	declarationScript
	| declarationModule;

declarationModule: unimplemented;

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
//	| typeAliasDeclaration
//	| namespaceDeclaration
//	| ambientDeclaration
//	| importAliasDeclaration
	;



interfaceDeclaration:
   'interface' bindingIdentifier (typeParameters)? (interfaceExtendsClause)? objectType
   ;

interfaceExtendsClause:
   'extends' ClassOrInterfaceTypeList
   ;

classOrInterfaceTypeList:
   classOrInterfaceType
   | classOrInterfaceTypeList ',' classOrInterfaceType
   ;

classOrInterfaceType:
	typeReference
	;

typeReference:
//   typeName /*[no LineTerminator here]*/ (typeArguments)?
	;

typeName:
   IdentifierReference
   | namespaceName '.' IdentifierReference
   ;

namespaceName:
   IdentifierReference
   | namespaceName '.' IdentifierReference
   ;

typeParameters:
	'<' typeParameterList '>'
	;

typeParameterList:
//	TypeParameter
//	| TypeParameterList , TypeParameter
	;

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
//   | CallSignature
//   | ConstructSignature
//   | IndexSignature
//   | MethodSignature
   ;

propertySignature:
   propertyName ('?')? (typeAnnotation)?
   ;

typeAnnotation:
   ':' type
   ;

propertyName:
   IdentifierName
//   | StringLiteral
//   | NumericLiteral
	;

type:
   unionOrIntersectionOrPrimaryType
//   | functionType
//   | constructorType
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
//   | typeReference
//   | objectType
//   | arrayType
//   | tupleType
//   | typeQuery
//   | thisType
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
	
/*
Mutually left-recursive
unionType:
	unionOrIntersectionOrPrimaryType '|' intersectionOrPrimaryType
	;

intersectionType:
   intersectionOrPrimaryType '&' primaryType
	;
*/	

unimplemented : ; 

WS : [ \t\r\n]+ -> skip ;

COMMENT : ('/*' .*? '*/'
		| '//' ~[\r\n]*)
		 -> channel(HIDDEN);
