package com.user00.domjnate.generator.ast;

import java.util.List;

public class TypeReference extends Type
{
   public String typeName;
   public List<Type> typeArgs;
   
   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitTypeReferenceType(this);
   }

}
