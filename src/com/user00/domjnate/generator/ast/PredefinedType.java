package com.user00.domjnate.generator.ast;

public class PredefinedType extends Type
{
   public String type;
   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitPredefinedType(this);
   }
}
