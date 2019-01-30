package com.user00.domjnate.generator.ast;

public class ArrayType extends Type
{
   public Type type;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitArrayType(this);
   }

   @Override
   public <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in)
   {
      return visitor.visitArrayType(this, in);
   }
}
