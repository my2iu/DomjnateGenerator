package com.user00.domjnate.generator.ast;

import java.util.List;

public class UnionType extends Type
{
   public List<Type> subtypes;

   public boolean isStringLiteralUnion()
   {
      for (Type t: subtypes)
      {
         if (!(t instanceof StringLiteralType))
            return false;
      }
      return true;
   }
   
   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitUnionType(this);
   }
}
