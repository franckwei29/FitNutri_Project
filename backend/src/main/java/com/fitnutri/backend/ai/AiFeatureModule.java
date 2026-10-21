package com.fitnutri.backend.ai;
/** Only canonical integrated adapters implement this marker; legacy adapters are not registered. No network probe. */
@FunctionalInterface
public interface AiFeatureModule {AiFeature feature();default boolean ready(){return true;}}
