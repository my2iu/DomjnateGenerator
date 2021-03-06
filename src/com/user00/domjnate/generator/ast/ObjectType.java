package com.user00.domjnate.generator.ast;

public class ObjectType extends Type
{
   public InterfaceDefinition intf;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitObjectType(this);
   }

   @Override
   public <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in)
   {
      return visitor.visitObjectType(this, in);
   }
}
