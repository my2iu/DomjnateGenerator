package com.user00.domjnate.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.ArrayType;
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.FunctionType;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.StringLiteralType;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.ast.UnionType;
import com.user00.domjnate.generator.tsparser.TsDeclarationsReader;
import com.user00.domjnate.generator.tsparser.TsIdlParser;

public class DomjnateGenerator
{
   String outputDir = "apigen";
   String pkg = "com.user00.domjnate.api";
   
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
      
      // Figure out what the static part of interfaces should be
      new FindStaticInterfaces(api).go(api);
      
      // Move lambdas to be proper function interfaces
      new ExtractFunctionInterfaces().convertFunctions(api);
      
      // Figure out the packages for all the interfaces
      new AssignPackages(api, pkg).go(api);
      
      // Generate JsInterop API based on type data that we've read
      generator.topLevel = api;
      generator.basePkg = pkg;
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
               method -> "getElementsByTagNameNS".equals(method.name) && method.callSigType.params.get(0).type instanceof StringLiteralType);
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
         
         // concat accepts T or Array<T>, but I'll make it accept on or the either but not both
         api.interfaces.get("Array").methods.forEach(method -> {
            if ("concat".equals(method.name) && ((ArrayType)method.callSigType.restParameter.type).type instanceof UnionType)
            {
               ((ArrayType)method.callSigType.restParameter.type).type = ((UnionType)((ArrayType)method.callSigType.restParameter.type).type).subtypes.get(1);
            }});
      }
      if (api.interfaces.containsKey("ReadonlyArray"))
      {
         // Some methods of ReadonlyArray seem to overlap and are unnecessary
         api.interfaces.get("ReadonlyArray").methods.removeIf(
               method -> "reduceRight".equals(method.name) && method.callSigType.params.size() == 2 && method.callSigType.genericTypeParameters == null); 
         api.interfaces.get("ReadonlyArray").methods.removeIf(
               method -> "reduce".equals(method.name) && method.callSigType.params.size() == 2 && method.callSigType.genericTypeParameters == null); 
         
         // TODO: write a new filter() that only accepts bools instead of truthy values
         api.interfaces.get("ReadonlyArray").methods.removeIf(
               method -> "filter".equals(method.name) && method.callSigType.genericTypeParameters != null);
         
         // concat accepts T or Array<T>, but I'll make it accept on or the either but not both
         api.interfaces.get("ReadonlyArray").methods.forEach(method -> {
            if ("concat".equals(method.name) && ((ArrayType)method.callSigType.restParameter.type).type instanceof UnionType)
            {
               ((ArrayType)method.callSigType.restParameter.type).type = ((UnionType)((ArrayType)method.callSigType.restParameter.type).type).subtypes.get(1);
            }});
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
               method -> "createElementNS".equals(method.name) && method.callSigType.params.get(0).type instanceof StringLiteralType);
         api.interfaces.get("Document").methods.removeIf(
               method -> "createEvent".equals(method.name) && method.callSigType.params.get(0).type instanceof StringLiteralType);
         // Remove some other overlapping methods
         api.interfaces.get("Document").methods.removeIf(   // 2 versions with almost identical parameters
               method -> "createElementNS".equals(method.name) && method.callSigType.params.size() == 2 
                     && method.callSigType.optionalParams.size() == 1 && method.callSigType.optionalParams.get(0).type instanceof TypeReference);
         api.interfaces.get("Document").methods.removeIf(
               method -> "createTreeWalker".equals(method.name) && method.callSigType.params.size() == 3 
                     && method.callSigType.optionalParams.size() == 1);   // deprecated
         api.interfaces.get("Document").methods.removeIf(
               method -> "getElementsByTagNameNS".equals(method.name) 
                     && method.callSigType.params.get(0).type instanceof StringLiteralType);   // getElementsByTagNameNS with constant string for namespace
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

      if (api.interfaces.containsKey("SubtleCrypto"))
      {
         // SubtleCrypto exportKey() has many variants for hard-coded strings
         api.interfaces.get("SubtleCrypto").methods.removeIf(
               method -> "exportKey".equals(method.name) && !(method.callSigType.params.get(0).type instanceof PredefinedType));
         
         // Also has unnecessary variants for different parameters
         api.interfaces.get("SubtleCrypto").methods.removeIf(
               method -> "importKey".equals(method.name) && !(method.callSigType.params.get(0).type instanceof PredefinedType));
      }

      // Remove variants with hardcoded strings from HTMLCanvasElement.getContext()
      if (api.interfaces.containsKey("HTMLCanvasElement"))
      {
         api.interfaces.get("HTMLCanvasElement").methods.removeIf(
               method -> "getContext".equals(method.name) && !(method.callSigType.params.get(0).type instanceof PredefinedType));
      }

      // Remove variants with hardcoded strings from DocumentEvent
      if (api.interfaces.containsKey("DocumentEvent"))
      {
         api.interfaces.get("DocumentEvent").methods.removeIf(
               method -> "createEvent".equals(method.name) && !(method.callSigType.params.get(0).type instanceof PredefinedType));
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
      
      // The type of Element.returnValue clashes with BeforeUnloadEvent.returnValue
      // Event.returnValue is never used in practice, and it's deprecated, so I'll just remove it to resolve the conflict
      if (api.interfaces.containsKey("Event"))
      {
         api.interfaces.get("Event").properties.removeIf(
               prop -> prop.name.equals("returnValue"));
      }
      
      // MediaList seems to have a toString() method that returns number for some reason.
      // I'll just remove it entirely.
      if (api.interfaces.containsKey("MediaList"))
      {
         api.interfaces.get("MediaList").methods.removeIf(
               method -> method.name.equals("toString"));
      }
      
      // QueuingStrategy.highWaterMark is optional (and hence nullable) while it's not optional in its subclasses
      // I will make it optional in the subclasses too so that it will be nullable everywhere.
      if (api.interfaces.containsKey("ByteLengthQueuingStrategy"))
      {
         api.interfaces.get("ByteLengthQueuingStrategy").properties.forEach(
               prop -> {
                  if (prop.name.equals("highWaterMark"))
                     prop.optional = true;
               });
      }
      if (api.interfaces.containsKey("CountQueuingStrategy"))
      {
         api.interfaces.get("CountQueuingStrategy").properties.forEach(
               prop -> {
                  if (prop.name.equals("highWaterMark"))
                     prop.optional = true;
               });
      }
      
      // JSON has two stringify methods that if all optional parameters are removed give two identical methods
      if (api.interfaces.containsKey("JSON"))
      {
         // Create a new stringify() method with just the mandatory parameters
         PropertyDefinition stringify = new PropertyDefinition();
         stringify.name = "stringify";
         stringify.callSigType = new CallSignatureDefinition();
         PredefinedType returnType = new PredefinedType();
         returnType.type = "string";
         stringify.callSigType.returnType = returnType;
         CallSignatureDefinition.CallParameter param = new CallSignatureDefinition.CallParameter();
         param.name = "value";
         PredefinedType anyType = new PredefinedType();
         anyType.type = "any";
         param.type = anyType;
         stringify.callSigType.params.add(param);
         api.interfaces.get("JSON").methods.add(stringify);
         
         // Remove the first optional parameter of the other methods since we already handle the case where they are optional with the new method
         api.interfaces.get("JSON").methods.forEach(method -> {
            if (method.name.equals("stringify") && method.callSigType.optionalParams.size() > 0)
               method.callSigType.params.add(method.callSigType.optionalParams.remove(0));
         });
      }
      
      // RegExp has two call signatures that result in duplicate methods when the optional parameters in one call signature is removed
      if (api.interfaces.containsKey("RegExpConstructor"))
      {
         // Remove the optional parameter
         api.interfaces.get("RegExpConstructor").callSignatures.forEach(callSigType -> {
            if (callSigType.optionalParams.size() == 1)
            {
               callSigType.params.add(callSigType.optionalParams.get(0));
               callSigType.optionalParams.clear();
            }
         });
         api.interfaces.get("RegExpConstructor").constructSignatures.forEach(callSigType -> {
            if (callSigType.optionalParams.size() == 1)
            {
               callSigType.params.add(callSigType.optionalParams.get(0));
               callSigType.optionalParams.clear();
            }
         });
      }
      
      // Properties whose value are the literal string "bytes" will be changed to have type string
      if (api.interfaces.containsKey("UnderlyingByteSource"))
      {
         PredefinedType stringType = new PredefinedType();
         stringType.type = "string";
         api.interfaces.get("UnderlyingByteSource").properties.forEach(
               prop -> {
                  if (prop.name.equals("type"))
                     prop.type = stringType;
               });
      }
      
      // PropertyDescriptor has optional methods which will be recoded as function types instead
      if (api.interfaces.containsKey("PropertyDescriptor"))
      {
         PredefinedType stringType = new PredefinedType();
         stringType.type = "string";
         api.interfaces.get("PropertyDescriptor").methods.forEach(
               method -> {
                  if (method.name.equals("get") || method.name.equals("set"))
                  {
                     FunctionType fnType = new FunctionType();
                     fnType.callSigType = method.callSigType;
                     fnType.callSigType.problems.problems.clear();
                     PropertyDefinition prop = new PropertyDefinition();
                     prop.name = method.name;
                     prop.type = fnType; 
                     api.interfaces.get("PropertyDescriptor").properties.add(prop);
                  }
               });
         api.interfaces.get("PropertyDescriptor").methods.removeIf(
               method -> method.name.equals("get") || method.name.equals("set")); 
      }

      // ReadableStream has a method that uses a bindingPattern, which we replace with a proper
      // variable name
      if (api.interfaces.containsKey("ReadableStream"))
      {
         api.interfaces.get("ReadableStream").methods.forEach(method -> {
            if (method.name.equals("pipeThrough") && method.callSigType.params.get(0).name.equals("_val"))
               method.callSigType.params.get(0).name = "transformStream";
         });
      }

      // Promise has arrays of different generics, which can't really be typed properly. So we can remove those extra versions
      if (api.interfaces.containsKey("PromiseConstructor"))
      {
         api.interfaces.get("PromiseConstructor").methods.removeIf(method ->
            method.name.equals("all") && method.callSigType.genericTypeParameters.size() > 1);
         api.interfaces.get("PromiseConstructor").methods.removeIf(method ->
            method.name.equals("race") && method.callSigType.genericTypeParameters.size() > 1);
      }
      
      // Adjust inheritance hierarchy of typed arrays
      for (String typedArray: Arrays.asList("Int8Array", "Uint8Array", "Uint8ClampedArray", "Int16Array", "Uint16Array", "Int32Array", "Uint32Array", "Float32Array", "Float64Array", "DataView"))
      {
         TypeReference arrayBufferView = new TypeReference();
         arrayBufferView.typeName = "ArrayBufferView";
         if (api.interfaces.containsKey(typedArray))
         {
            if (api.interfaces.get(typedArray).extendsTypes == null)
               api.interfaces.get(typedArray).extendsTypes = new ArrayList<>();
            if (api.interfaces.get(typedArray).extendsTypes.size() == 0)
               api.interfaces.get(typedArray).extendsTypes.add(arrayBufferView);
         }
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
            "WritableStreamErrorCallback",
            "Transformer",
            "TransformStream",
            "TransformStreamDefaultController",
            "TransformStreamDefaultControllerCallback",
            "TransformStreamDefaultControllerTransformCallback",
            "UnderlyingByteSource",
            "UnderlyingSink",
            "UnderlyingSource"
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
            "RsaPssParams",
            "KeyAlgorithm"
            );
      Set<String> mediaSourceExtensionsClasses = Set.of(
            "MediaSource",
            "SourceBuffer",
            "SourceBufferList",
            "VideoPlaybackQuality"
            );
      Set<String> mediaCaptureAndStreamsClasses = Set.of(
            "MediaDeviceInfo",
            "MediaDevices",
            "MediaDevicesEventMap",
            "MediaStream",
            "MediaStreamConstraints",
            "MediaStreamError",
            "MediaStreamErrorEvent",
            "MediaStreamErrorEventInit",
            "MediaStreamEvent",
            "MediaStreamEventInit",
            "MediaStreamEventMap",
            "MediaStreamTrack",
            "MediaStreamTrackAudioSourceNode",
            "MediaStreamTrackAudioSourceOptions",
            "MediaStreamTrackEvent",
            "MediaStreamTrackEventInit",
            "MediaStreamTrackEventMap",
            "MediaTrackCapabilities",
            "MediaTrackConstraints",
            "MediaTrackConstraintSet",
            "MediaTrackSettings",
            "MediaTrackSupportedConstraints",
            "NavigatorUserMedia",
            "NavigatorUserMediaErrorCallback",
            "NavigatorUserMediaSuccessCallback",
            "ConstrainBooleanParameters",
            "ConstrainDOMStringParameters",
            "ConstrainDoubleRange",
            "ConstrainLongRange",
            "ConstrainVideoFacingModeParameters",
            "DoubleRange",
            "LongRange"
            );
      for (InterfaceDefinition intf: api.interfaces.values())
      {
         if (domClasses.contains(intf.name))
            intf.remapPackage = "dom";
         else if (intf.name.startsWith("HTML") || intf.name.equals("GetSVGDocument"))
            intf.remapPackage = "html";
         else if (intf.name.startsWith("SVG"))
            intf.remapPackage = "svg";
         else if (intf.name.startsWith("RTC") || intf.name.equals("webkitRTCPeerConnection"))
            intf.remapPackage = "webrtc";
         else if (intf.name.startsWith("IDB"))
            intf.remapPackage = "indexeddb";
         else if (intf.name.startsWith("Speech"))
            intf.remapPackage = "webspeech";
         else if (streamClasses.contains(intf.name))
            intf.remapPackage = "stream";
         else if (cryptoClasses.contains(intf.name))
            intf.remapPackage = "webcrypto";
         else if (mediaSourceExtensionsClasses.contains(intf.name))
            intf.remapPackage = "mse";
         else if (mediaCaptureAndStreamsClasses.contains(intf.name))
            intf.remapPackage = "mediastream";
         else if (intf.name.startsWith("Audio") || webAudioClasses.contains(intf.name))
            intf.remapPackage = "webaudio";
         else if (intf.name.startsWith("Performance"))
            intf.remapPackage = "performance";
         else if (intf.name.startsWith("VTT") || intf.name.startsWith("TextTrack"))
            intf.remapPackage = "webvtt";
         else if (intf.name.startsWith("MediaKey") || intf.name.startsWith("MediaEncrypted"))
            intf.remapPackage = "eme";
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
