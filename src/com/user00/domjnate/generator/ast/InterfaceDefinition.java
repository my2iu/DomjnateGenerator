package com.user00.domjnate.generator.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

   public String remapPackage = null;  // Changes the package where Java interface will be put
   public boolean doNotGenerateJava = false;  // Do not generate a Java interface for this interface
   
   // Extra interface information derived by transforming the TypeScript idl to make it more appropriate for Java
   public InterfaceDefinition staticIntf;  // Interface that holds the static methods and properties of an object
   public boolean isStaticOnly = false;       // Do not generate any instance properties or methods for the interface since it's not possible to instantiate the object
   public Map<String, LocalFunctionDefinition> functionTypes = new HashMap<>();  // Function types used specifically in this interface
   public String finalPkg;  // final package where interface should be placed
   
   public boolean isFunction()
   {
      return (properties.isEmpty() && methods.isEmpty() && !callSignatures.isEmpty());
   }
}
