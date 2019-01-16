package com.user00.domjnate.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.ErrorType;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.tsparser.TsDeclarationsReader;
import com.user00.domjnate.generator.tsparser.TsIdlParser;

public class DomjnateGenerator
{
   String outputDir = "apigen";
   public void go() throws IOException
   {
      ApiGenerator generator = new ApiGenerator();
      generator.outputDir = outputDir;
      go(new CharStream[] {
            CharStreams.fromFileName("idl/lib.dom.d.ts"),     
            CharStreams.fromFileName("idl/lib.es5.d.ts"),     
            CharStreams.fromFileName("idl/lib.es2015.promise.d.ts"),     
            CharStreams.fromFileName("idl/lib.es2015.symbol.d.ts"),     
      }, generator);
   }

   public void go(CharStream[] inputStreams, ApiGenerator generator) throws IOException
   {
      // Read out type information for DOM
      ApiDefinition api = new ApiDefinition(null);
      for (CharStream input: inputStreams)
      {
         TsIdlParser.DeclarationSourceFileContext libDomTs = TsDeclarationsReader.parseTs(input);
         libDomTs.accept(new TsDeclarationsReader.TopLevelReader(api));
      }
      
      // Apply changes
      fixupApi(api);
      
      // Generate JsInterop API based on type data that we've read
      generator.topLevel = api;
      generator.generate(null, api);
   }
   
   void fixupApi(ApiDefinition api)
   {
      // Ignore the fact that objects can be used for event listeners and allow for lambdas only
      TypeReference eventListenerOnly = new TypeReference();
      eventListenerOnly.typeName = "EventListener";
      api.typeAliases.put("EventListenerOrEventListenerObject", eventListenerOnly);

      // Remove some extra getElementsByTagNameNS() methods on Element
      if (api.interfaces.containsKey("Element"))
      {
         api.interfaces.get("Element").methods.removeIf(
               method -> "getElementsByTagNameNS".equals(method.name) && method.callSigType.params.get(0).type instanceof ErrorType);
      }

      if (api.interfaces.containsKey("Array"))
      {
         // Some methods of Array seem to overlap and are unnecessary
         api.interfaces.get("Array").methods.removeIf(
               method -> "reduceRight".equals(method.name) && method.callSigType.params.size() == 2 && method.callSigType.genericTypeParameters == null); 
         api.interfaces.get("Array").methods.removeIf(
               method -> "reduce".equals(method.name) && method.callSigType.params.size() == 2 && method.callSigType.genericTypeParameters == null); 
         
         // TODO: write a new filter() that only accepts bools instead of truthy values
         api.interfaces.get("Array").methods.removeIf(
               method -> "filter".equals(method.name) && method.callSigType.genericTypeParameters != null); 
      }
      if (api.interfaces.containsKey("ArrayConstructor"))
      {
         // Constructors come in generic version and "any" version. Remove the "any" version so everything is properly typed
         api.interfaces.get("ArrayConstructor").constructSignatures.removeIf(
               construct -> construct.genericTypeParameters == null); 
         api.interfaces.get("ArrayConstructor").callSignatures.removeIf(
               call -> call.genericTypeParameters == null); 
      }
      
      // Document has some methods that overlap
      if (api.interfaces.containsKey("Document"))
      {
         // Remove some extra createEvent() and createElementNS() methods on Document
         api.interfaces.get("Document").methods.removeIf(
               method -> "createElementNS".equals(method.name) && method.callSigType.params.get(0).type instanceof ErrorType);
         api.interfaces.get("Document").methods.removeIf(
               method -> "createEvent".equals(method.name) && method.callSigType.params.get(0).type instanceof ErrorType);
         // Remove some other overlapping methods
         api.interfaces.get("Document").methods.removeIf(   // 2 versions with almost identical parameters
               method -> "createElementNS".equals(method.name) && method.callSigType.params.size() == 2 
                     && method.callSigType.optionalParams.size() == 1 && method.callSigType.optionalParams.get(0).type instanceof TypeReference);
         api.interfaces.get("Document").methods.removeIf(
               method -> "createTreeWalker".equals(method.name) && method.callSigType.params.size() == 3 
                     && method.callSigType.optionalParams.size() == 1);   // deprecated
         api.interfaces.get("Document").methods.removeIf(
               method -> "getElementsByTagNameNS".equals(method.name) 
                     && method.callSigType.params.get(0).type instanceof ErrorType);   // getElementsByTagNameNS with constant string for namespace
         api.interfaces.get("Document").properties.forEach(
               prop -> {
                  if (!prop.name.equals("links")) return;
                  TypeReference commonSuperType = new TypeReference();
                  commonSuperType.typeName = "HTMLHyperlinkElementUtils";
                  ((TypeReference)prop.type).typeArgs.set(0, commonSuperType); 
               });   // returning a union of two types is messy
      }
      
      // Make HTMLHyperlinkElementUtils also inherit from HTMLElement so that it can be put in an HTMLCollectionOf<> for Document.links
      if (api.interfaces.containsKey("HTMLHyperlinkElementUtils"))
      {
         TypeReference fixUpDocumentLinks = new TypeReference();
         fixUpDocumentLinks.typeName = "HTMLElement";
         if (api.interfaces.get("HTMLHyperlinkElementUtils").extendsTypes == null)
            api.interfaces.get("HTMLHyperlinkElementUtils").extendsTypes = new ArrayList<>();
         api.interfaces.get("HTMLHyperlinkElementUtils").extendsTypes.add(fixUpDocumentLinks);
      }
      
      // SVGElement.className is deprecated, and it conflicts with Element.className
      if (api.interfaces.containsKey("SVGElement"))
      {
         api.interfaces.get("SVGElement").properties.removeIf( p -> "className".equals(p.name));
      }
      
      // Interfaces defined multiple times with different capitalizations (remove the one with the incorrect capitalization and incorrect definition)
      api.interfaces.remove("RTCDtmfSender");
      api.interfaces.remove("RTCDtmfSenderEventMap");
      
      // Remove the String type
      api.interfaces.remove("String");
      api.interfaces.remove("StringConstructor");
      
      // WebGLRenderingContext getExtension() has many variants for hard-coded strings
      if (api.interfaces.containsKey("WebGLRenderingContextBase"))
      {
         api.interfaces.get("WebGLRenderingContextBase").methods.removeIf(
               method -> "getExtension".equals(method.name) && !(method.callSigType.params.get(0).type instanceof PredefinedType));
      }
      
      // Remove variants with hardcoded strings from HTMLCanvasElement.getContext()
      if (api.interfaces.containsKey("HTMLCanvasElement"))
      {
         api.interfaces.get("HTMLCanvasElement").methods.removeIf(
               method -> "getContext".equals(method.name) && !(method.callSigType.params.get(0).type instanceof PredefinedType));
      }
      
      // SVGSVGElement returns a list of a union type, but union types aren't supported in java
      if (api.interfaces.containsKey("SVGSVGElement"))
      {
         api.interfaces.get("SVGSVGElement").methods.forEach(
               method -> {
                  if (!method.name.equals("getEnclosureList") && !method.name.equals("getIntersectionList")) return;
                  TypeReference superType = new TypeReference();
                  superType.typeName = "SVGGraphicsElement";
                  ((TypeReference)method.callSigType.returnType).typeArgs.set(0, superType);
               });
      }
      
      // Avoid union types in HTMLTableRowElement
      if (api.interfaces.containsKey("HTMLTableRowElement"))
      {
         api.interfaces.get("HTMLTableRowElement").properties.forEach(
               prop -> {
                  if (!prop.name.equals("cells")) return;
                  TypeReference superType = new TypeReference();
                  superType.typeName = "HTMLTableCellElement";
                  ((TypeReference)prop.type).typeArgs.set(0, superType);
               });
      }
      
      // HTMLCollectionOf<> and NodeListOf<> clash a little bit with their superclass, but this superclass isn't needed since anything can be cast to anything else
      if (api.interfaces.containsKey("HTMLCollectionOf"))
      {
         api.interfaces.get("HTMLCollectionOf").extendsTypes = null;
      }
      if (api.interfaces.containsKey("NodeListOf"))
      {
         api.interfaces.get("NodeListOf").extendsTypes = null;
      }
      
      // HTMLEmbedElement declares a hidden property with type any while HTMLElement has a hidden property of type boolean. 
      // I'll just remove it. The alternate definition doesn't seem to be listed in the MDN docs
      if (api.interfaces.containsKey("HTMLEmbedElement"))
      {
         api.interfaces.get("HTMLEmbedElement").properties.removeIf(
               prop -> prop.name.equals("hidden"));
      }
      
      // ShadowRoot inherits from DocumentOrShadowRoot twice
      if (api.interfaces.containsKey("ShadowRoot"))
      {
         if (api.interfaces.get("ShadowRoot").extendsTypes.get(2).equals(api.interfaces.get("ShadowRoot").extendsTypes.get(0)))
            api.interfaces.get("ShadowRoot").extendsTypes.remove(2);
      }
      
      // Remove artificial interfaces used to store constructors and static methods
      for (String intfName: Arrays.asList(
            "URIErrorConstructor",
            "PromiseConstructor",
            "Int8ArrayConstructor",
            "Float32ArrayConstructor",
            "Uint8ClampedArrayConstructor",
            "FunctionConstructor",
            "ArrayConstructor",
            "TypeErrorConstructor",
            "ArrayBufferConstructor",
            "Uint32ArrayConstructor",
            "Uint16ArrayConstructor",
            "BooleanConstructor",
            "ObjectConstructor",
            "SyntaxErrorConstructor",
            "Int32ArrayConstructor",
            "RangeErrorConstructor",
            "DateConstructor",
            "DataViewConstructor",
            "ReferenceErrorConstructor",
            "RegExpConstructor",
            "NumberConstructor",
            "Int16ArrayConstructor",
            "EvalErrorConstructor",
            "SymbolConstructor",
            "ErrorConstructor",
            "Uint8ArrayConstructor",
            "Float64ArrayConstructor"))
      {
         if (api.interfaces.containsKey(intfName))
         {
            api.interfaces.get(intfName).doNotGenerateJava = true;
         }
      }
      
      // Remap Intl namespace to lower-case intl
      if (api.namespaces.containsKey("Intl"))
         api.namespaces.get("Intl").remapName = "intl";
      
      // Move some classes into different packages so that things are more manageable
      Set<String> domClasses = Set.of("Attr", "CDATASection", "CharacterData", 
            "Comment", "Document", "DocumentFragment", "DocumentType", 
//            "DOMConfiguration", 
            "DOMError", 
//            "DOMErrorHandler", 
            "DOMImplementation", 
//            "DOMImplementationList", "DOMImplementationSource", "DOMLocator", 
            "DOMStringList", "Element", 
//            "Entity", "EntityReference", 
            "NamedNodeMap", 
//            "NameList", 
            "Node", "NodeList", 
//            "Notation", 
            "ProcessingInstruction", "Text", 
//            "TypeInfo", "UserDataHandler", 
            "DOMException",
            
            "ParentNode",
            "ChildNode",
            "CustomEvent",
            "CustomEventInit",
            "DOMTokenList",
            "Event",
            "EventInit",
            "EventTarget",
            "MutationObserver",
            "MutationObserverInit",
            "MutationRecord",
            "NodeFilter",
            "NodeIterator",
            "NodeListOf",
            "NonDocumentTypeChildNode",
            "Range",
            "TimeRanges",
            "TreeWalker",
            "HTMLCollection",
            "HTMLCollectionBase",
            "HTMLCollectionOf",
            "ElementCreationOptions"
            );
      Set<String> webAudioClasses = Set.of("AnalyserNode", "AnalyserOptions",
            "BaseAudioContext", "BaseAudioContextEventMap", "BiquadFilterNode", "BiquadFilterOptions",
            "ChannelMergerNode", "ChannelMergerOptions", "ChannelSplitterNode", "ChannelSplitterOptions",
            "ConstantSourceNode", 
            "ConstantSourceOptions",
            "ConvolverNode",
            "ConvolverOptions",
            "DelayNode",
            "DelayOptions",
            "DynamicsCompressorNode",
            "DynamicsCompressorOptions",
            "GainNode",
            "GainOptions",
            "IIRFilterNode",
            "IIRFilterOptions",
            "MediaElementAudioSourceNode",
            "MediaElementAudioSourceOptions",
            "MediaStreamAudioDestinationNode",
            "MediaStreamAudioSourceNode",
            "MediaStreamAudioSourceOptions",
            "OfflineAudioCompletionEvent",
            "OfflineAudioCompletionEventInit",
            "OfflineAudioContext",
            "OfflineAudioContextEventMap",
            "OfflineAudioContextOptions",
            "OscillatorNode",
            "OscillatorOptions",
            "PannerNode",
            "PannerOptions",
            "PeriodicWave",
            "PeriodicWaveConstraints",
            "PeriodicWaveOptions",
            "StereoPannerNode",
            "StereoPannerOptions",
            "WaveShaperNode",
            "WaveShaperOptions"
            );
      Set<String> streamClasses = Set.of(
            "ByteLengthQueuingStrategy",
            "CountQueuingStrategy",
            "ReadableByteStreamController",
            "ReadableStream",
            "ReadableStreamBYOBReader",
            "ReadableStreamBYOBRequest",
            "ReadableStreamDefaultController",
            "ReadableStreamDefaultReader",
            "WritableStream",
            "WritableStreamDefaultController",
            "WritableStreamDefaultWriter",
            
            "QueuingStrategy",
            "QueuingStrategySizeCallback",
            "ReadableByteStreamControllerCallback",
            "ReadableStreamDefaultControllerCallback",
            "ReadableStreamErrorCallback",
            "ReadableStreamReader",
            "ReadableStreamReadResult",
            "WritableStreamDefaultControllerCloseCallback",
            "WritableStreamDefaultControllerStartCallback",
            "WritableStreamDefaultControllerWriteCallback",
            "WritableStreamErrorCallback"
            );
      Set<String> cryptoClasses = Set.of(
            "Algorithm",
            "Crypto",
            "CryptoKey",
            "CryptoKeyPair",
            "RandomSource",
            "SubtleCrypto",
            "AesCbcParams",
            "AesCfbParams",
            "AesCmacParams",
            "AesCtrParams",
            "AesDerivedKeyParams",
            "AesGcmParams",
            "AesKeyAlgorithm",
            "AesKeyGenParams",
            "ConcatParams",
            "DhImportKeyParams",
            "DhKeyAlgorithm",
            "DhKeyDeriveParams",
            "DhKeyGenParams",
            "EcdhKeyDeriveParams",
            "EcdsaParams",
            "EcKeyAlgorithm",
            "EcKeyGenParams",
            "EcKeyImportParams",
            "HkdfCtrParams",
            "HkdfParams",
            "HmacImportParams",
            "HmacKeyAlgorithm",
            "HmacKeyGenParams",
            "Pbkdf2Params",
            "RsaHashedImportParams",
            "RsaHashedKeyAlgorithm",
            "RsaHashedKeyGenParams",
            "RsaKeyAlgorithm",
            "RsaKeyGenParams",
            "RsaOaepParams",
            "RsaOtherPrimesInfo",
            "RsaPssParams"            
            );
      for (InterfaceDefinition intf: api.interfaces.values())
      {
         if (domClasses.contains(intf.name))
            intf.remapPackage = "dom";
         else if (intf.name.startsWith("HTML"))
            intf.remapPackage = "html";
         else if (intf.name.startsWith("SVG"))
            intf.remapPackage = "svg";
         else if (intf.name.startsWith("RTC"))
            intf.remapPackage = "webrtc";
         else if (intf.name.startsWith("Speech"))
            intf.remapPackage = "webspeech";
         else if (streamClasses.contains(intf.name))
            intf.remapPackage = "stream";
         else if (cryptoClasses.contains(intf.name))
            intf.remapPackage = "webcrypto";
         else if (intf.name.startsWith("Audio") || webAudioClasses.contains(intf.name))
            intf.remapPackage = "webaudio";
         else if (intf.name.startsWith("WebGL") || intf.name.startsWith("WEBGL") || intf.name.startsWith("OES_") || intf.name.startsWith("EXT_")  || intf.name.startsWith("ANGLE_"))
            intf.remapPackage = "webgl";
      }
   }


   public static void main(String [] args) throws IOException
   {
      DomjnateGenerator generator = new DomjnateGenerator();
      if (args.length > 0)
         generator.outputDir = args[0];
      generator.go();
   }
}
