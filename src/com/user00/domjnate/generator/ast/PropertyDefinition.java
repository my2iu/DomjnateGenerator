package com.user00.domjnate.generator.ast;

public class PropertyDefinition
{
   public String name;
   public boolean readOnly = false;
   public boolean optional = false;
   
   public Type type;
   public CallSignatureDefinition callSigType;  // Only used for methods (not function properties)
   
   public ProblemTracker problems = new ProblemTracker();
}
