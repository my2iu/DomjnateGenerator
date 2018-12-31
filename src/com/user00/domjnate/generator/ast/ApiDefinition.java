package com.user00.domjnate.generator.ast;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds all the JsInterop definitions.
 */
public class ApiDefinition
{
   public Map<String, InterfaceDefinition> interfaces = new HashMap<>();
   public ProblemTracker problems = new ProblemTracker();
   public Map<String, Type> typeAliases = new HashMap<>();
   public Map<String, Type> ambientVars = new HashMap<>();
   public Map<String, Type> ambientConsts = new HashMap<>();
}
