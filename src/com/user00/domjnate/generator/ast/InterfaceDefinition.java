package com.user00.domjnate.generator.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a JsInterop interface
 */
public class InterfaceDefinition
{
   public String name;
   public List<TypeReference> extendsTypes;
   public List<GenericParameter> genericTypeParams;
   
   public List<PropertyDefinition> properties = new ArrayList<>();
   public List<PropertyDefinition> methods = new ArrayList<>();
   public List<CallSignatureDefinition> callSignatures = new ArrayList<>();
   public List<CallSignatureDefinition> constructSignatures = new ArrayList<>();
   
   public List<IndexSignatureDefinition> indexSignatures = new ArrayList<>();
   
   public ProblemTracker problems = new ProblemTracker();

   public boolean doNotGenerateJava = false;  // Do not generate a Java interface for this interface
   public boolean isStaticOnly = false;       // Do not generate any instance properties or methods for the interface since it's not possible to instantiate the object
   
   public boolean isFunction()
   {
      return (properties.isEmpty() && methods.isEmpty() && !callSignatures.isEmpty());
   }
}
