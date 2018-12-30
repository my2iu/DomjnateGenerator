package com.user00.domjnate.generator.ast;

public class IndexSignatureDefinition
{
   public String indexName;
   public Type indexType;
   public Type returnType;
   public boolean readOnly;

   public ProblemTracker problems = new ProblemTracker();
}
