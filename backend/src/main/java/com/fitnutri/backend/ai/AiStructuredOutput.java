package com.fitnutri.backend.ai;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fitnutri.backend.common.BusinessException;
import java.util.*;
import org.springframework.http.HttpStatus;

/** Model output is untrusted even with Structured Outputs; no tool output is executed. */
public final class AiStructuredOutput {
    private static final ObjectMapper JSON=new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(16).maxStringLength(64000).maxNumberLength(64).build()).build()).enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private AiStructuredOutput(){}
    public static ObjectNode object(AiTransport.Response response){
        JsonNode root=response.json(),output=root.get("output");
        if(!"completed".equals(root.path("status").asText())||output==null||!output.isArray()||output.isEmpty())throw AiExceptionMapper.badResponse();
        String text=null;int messages=0;
        for(JsonNode item:output){
            if("reasoning".equals(item.path("type").asText()))continue;
            if(!"message".equals(item.path("type").asText())||!"assistant".equals(item.path("role").asText())||!"completed".equals(item.path("status").asText())||++messages>1)throw AiExceptionMapper.badResponse();
            JsonNode content=item.get("content");if(content==null||!content.isArray()||content.isEmpty())throw AiExceptionMapper.badResponse();
            for(JsonNode part:content){
                if("refusal".equals(part.path("type").asText()))throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"PROVIDER_REFUSED","The AI service did not provide a result for this input. Try a different input.");
                if(!"output_text".equals(part.path("type").asText())||text!=null||!part.path("text").isTextual())throw AiExceptionMapper.badResponse();
                text=part.get("text").textValue();
            }
        }
        if(text==null||text.isBlank()||text.length()>64000)throw AiExceptionMapper.badResponse();
        try{JsonNode parsed=JSON.readTree(text);if(parsed instanceof ObjectNode object)return object;}catch(Exception ignored){/* No provider text/cause escapes. */}
        throw AiExceptionMapper.badResponse();
    }
    public static void requireFields(JsonNode value,Set<String> expected){
        if(value==null||!value.isObject())throw AiExceptionMapper.badResponse();
        var names=new HashSet<String>();value.fieldNames().forEachRemaining(names::add);if(!names.equals(expected))throw AiExceptionMapper.badResponse();
    }
}
