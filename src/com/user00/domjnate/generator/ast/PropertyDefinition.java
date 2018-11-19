package com.user00.domjnate.generator.ast;

public class PropertyDefinition
{
   public String name;
   public boolean readOnly = false;
   public boolean optional = false;
   
   public BasicJsType basicType;
   public CallSignatureDefinition callSigType;
   
   public ProblemTracker problems = new ProblemTracker();
}
