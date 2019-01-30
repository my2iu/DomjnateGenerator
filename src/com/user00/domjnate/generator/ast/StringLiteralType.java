package com.user00.domjnate.generator.ast;

public class StringLiteralType extends Type
{
   public String stringLiteral;

   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitStringLiteralType(this);
   }

   @Override
   public <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in)
   {
      return visitor.visitStringLiteralType(this, in);
   }
}
