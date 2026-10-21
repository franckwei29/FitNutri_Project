package com.fitnutri.backend.common;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.*;

/** Separate V2 canonical encoding: sorted object keys, preserved array order, exact normalized decimal text. V1 is untouched. */
@Component
public class CanonicalV2Json {
 private final ObjectMapper json;
 public CanonicalV2Json(ObjectMapper json){this.json=json;}
 public String canonical(Object value){return json.writeValueAsString(normalise(json.valueToTree(value)));}
 public String hash(String domain,Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((domain+"\n"+canonical(value)).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}}
 private Object normalise(JsonNode node){
  if(node.isNull())return null;if(node.isObject()){var result=new TreeMap<String,Object>();for(String name:node.propertyNames())result.put(name,normalise(node.get(name)));return result;}
  if(node.isArray()){var result=new ArrayList<Object>();for(var child:node)result.add(normalise(child));return result;}
  if(node.isNumber()){var number=new BigDecimal(node.asText());if(number.precision()>100||Math.abs((long)number.scale())>100)throw BusinessException.invalid("request","Use bounded decimal inputs.");return number.stripTrailingZeros().toPlainString();}
  if(node.isBoolean())return node.asBoolean();return node.asText();
 }
}
