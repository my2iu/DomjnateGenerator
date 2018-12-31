package com.user00.domjnate.generator.ast;

import java.util.List;

public class TypeReference extends Type
{
   public String typeName;
   public List<Type> typeArgs;

   
   @Override
   public boolean equals(Object obj)
   {
      // TODO: Implement this properly!
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      TypeReference other = (TypeReference) obj;
      if (typeArgs == null)
      {
         if (other.typeArgs != null)
            return false;
      } else if (!typeArgs.equals(other.typeArgs))
         return false;
      if (typeName == null)
      {
         if (other.typeName != null)
            return false;
      } else if (!typeName.equals(other.typeName))
         return false;
      return true;
   }

   
   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitTypeReferenceType(this);
   }

}
