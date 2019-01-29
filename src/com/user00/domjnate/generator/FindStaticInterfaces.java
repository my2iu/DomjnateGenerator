package com.user00.domjnate.generator;

import java.util.Map;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.ObjectType;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.TypeReference;

/**
 * Goes through all interfaces and tries to merge static methods and properties
 * with the instance version of the interfaces.
 */
public class FindStaticInterfaces
{
   ApiDefinition topLevel;
   
   public FindStaticInterfaces(ApiDefinition topLevel)
   {
      this.topLevel = topLevel;
   }
   
   void handleFunctionInterface(InterfaceDefinition intf, ApiDefinition api) 
   {
      
   }
   
   InterfaceDefinition lookupInterfaceAmbient(String name, Map<String, Type> ambientVars)
   {
      Type type = ambientVars.get(name);
      if (type instanceof ObjectType)
      {
         ambientVars.remove(name);
         return ((ObjectType)type).intf;
      }
      else if (type instanceof TypeReference && topLevel.interfaces.containsKey(((TypeReference)type).typeName))
      {
         if (!topLevel.interfaces.get(((TypeReference)type).typeName).doNotGenerateJava)
         {
            if (name.equals(((TypeReference)type).typeName))
               topLevel.interfaces.get(((TypeReference)type).typeName).isStaticOnly = true;
            else
               System.err.println("static ambient " + name + " with " + ((TypeReference)type).typeName);
         }
         ambientVars.remove(name);
         return topLevel.interfaces.get(((TypeReference)type).typeName); 
      }
      return null;
   }

   void handleInterface(ApiDefinition api, InterfaceDefinition intf) 
   {
      if (intf.doNotGenerateJava) return;
      if (intf.isFunction())
      {
         handleFunctionInterface(intf, api);
         return;
      }
      String name = intf.name;
      InterfaceDefinition intfAmbient = null;
      if (api.ambientVars.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, api.ambientVars);
      else if (api.ambientConsts.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, api.ambientConsts);
      else if (topLevel.ambientVars.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, topLevel.ambientVars);
      else if (topLevel.ambientConsts.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, topLevel.ambientConsts);
      intf.staticIntf = intfAmbient;
   }

   public void go(ApiDefinition api)
   {
      for (InterfaceDefinition intf: api.interfaces.values())
      {
         handleInterface(api, intf);
      }
      for (Map.Entry<String, ApiDefinition> namespace: api.namespaces.entrySet())
      {
         go(namespace.getValue());
      }
   }
}
