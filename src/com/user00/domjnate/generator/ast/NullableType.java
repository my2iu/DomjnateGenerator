package com.user00.domjnate.generator.ast;

public class NullableType extends Type
{
   public Type subtype;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitNullableType(this);
   }

   @Override
   public <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in)
   {
      return visitor.visitNullableType(this, in);
   }
}
