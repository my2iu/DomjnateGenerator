package com.user00.domjnate.generator.ast;

import java.util.List;

public class UnionType extends Type
{
   public List<Type> subtypes;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitUnionType(this);
   }
}
