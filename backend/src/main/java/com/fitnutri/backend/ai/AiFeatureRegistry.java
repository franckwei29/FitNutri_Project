package com.fitnutri.backend.ai;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
@Component
public class AiFeatureRegistry {
    private final ObjectProvider<AiFeatureModule> modules;
    public AiFeatureRegistry(ObjectProvider<AiFeatureModule> modules){this.modules=modules;}
    public boolean registered(AiFeature feature){return modules.stream().anyMatch(module->module.feature()==feature&&module.ready());}
}
