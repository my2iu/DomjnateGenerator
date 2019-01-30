package com.user00.domjnate.generator.ast;

public class TypeQueryType extends Type
{
   public String simpleType;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitTypeQueryType(this);
   }

   @Override
   public <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in)
   {
      return visitor.visitTypeQueryType(this, in);
   }

}
