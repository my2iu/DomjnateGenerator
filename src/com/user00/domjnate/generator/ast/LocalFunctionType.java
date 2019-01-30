package com.user00.domjnate.generator.ast;

/**
 * A synthetic type for a function interface defined as a nested class
 * of the interface.
 */
public class LocalFunctionType extends Type
{
   public String nestedName;
   public CallSignatureDefinition callSigType;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitLocalFunctionType(this);
   }
   
   @Override
   public <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in)
   {
      return visitor.visitLocalFunctionType(this, in);
   }
}
