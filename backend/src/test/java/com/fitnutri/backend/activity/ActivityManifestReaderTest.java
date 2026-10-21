package com.fitnutri.backend.activity;

import com.fitnutri.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ActivityManifestReaderTest {
    private final ActivityManifestReader reader=new ActivityManifestReader(JsonMapper.builder().build());
    private ActivityManifestReader.Prepared read(ActivityFixture.Input i) {return reader.prepare(i.manifest(),i.records(),i.sources());}
    @Test void explicitSourceAgeRangeAndUnmodifiedDecimalMetArePrepared() throws Exception {var p=read(ActivityFixture.input("one","synthetic","3.50"));assertEquals(19,p.manifest().minAge());assertEquals(59,p.manifest().maxAge());assertEquals("99901",p.activities().getFirst().sourceActivityCode());assertEquals(0,new java.math.BigDecimal("3.50").compareTo(p.activities().getFirst().metValue()));}
    @Test void wrongSourceBytesOrNormalizedBytesAreRejected() throws Exception {var i=ActivityFixture.input("one","synthetic","3.5");assertEquals("DATASET_SOURCE_INVALID",assertThrows(BusinessException.class,()->reader.prepare(i.manifest(),i.records(),Map.of("source.txt",new ByteArrayResource(new byte[]{1})))).code());assertThrows(BusinessException.class,()->reader.prepare(i.manifest(),new ByteArrayResource(new byte[]{1}),i.sources()));}
    @Test void missingSourceCodeBadMetAndDuplicateKeysFailBeforeTransactions() throws Exception {
        for(String value:new String[]{"0","-1","3.555","10000"}) assertThrows(BusinessException.class,()->read(ActivityFixture.input("one","synthetic",value)));
        var i=ActivityFixture.input("one","synthetic","3.5");assertThrows(BusinessException.class,()->read(ActivityFixture.transform(i,n->((ObjectNode)n.get("activities").get(0)).remove("sourceActivityCode"))));
        assertThrows(BusinessException.class,()->read(ActivityFixture.transform(i,n->((tools.jackson.databind.node.ArrayNode)n.get("activities")).add(n.get("activities").get(0).deepCopy()))));
    }
    @Test void unknownOrInvertedAgeRangeAndUnsafeFilenamesAreRejected() throws Exception {
        var i=ActivityFixture.input("one","synthetic","3.5");var json=JsonMapper.builder().build();
        for(String field:new String[]{"minAge","maxAge","recordsFile"}) {var m=(ObjectNode)json.readTree(i.manifest().getInputStream());if(field.equals("recordsFile"))m.put(field,"../activities.json");else if(field.equals("minAge"))m.remove(field);else m.put(field,18);assertThrows(BusinessException.class,()->reader.prepare(new ByteArrayResource(json.writeValueAsBytes(m)),i.records(),i.sources()));}
    }
}
