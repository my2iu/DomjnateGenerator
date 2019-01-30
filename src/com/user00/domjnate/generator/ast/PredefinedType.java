package com.user00.domjnate.generator.ast;

public class PredefinedType extends Type
{
   public String type;
   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitPredefinedType(this);
   }
   
   @Override
   public <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in)
   {
      return visitor.visitPredefinedType(this, in);
   }
}
