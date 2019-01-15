package com.user00.domjnate.generator.ast;

public class ArrayType extends Type
{
   public Type type;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitArrayType(this);
   }
}
