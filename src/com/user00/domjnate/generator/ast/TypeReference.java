package com.user00.domjnate.generator.ast;

public class TypeReference extends Type
{
   public String typeName;
   
   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitTypeReferenceType(this);
   }

}
