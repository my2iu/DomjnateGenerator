package com.user00.domjnate.generator.ast;

public class FunctionType extends Type
{
   public CallSignatureDefinition callSigType;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitFunctionType(this);
   }
}
