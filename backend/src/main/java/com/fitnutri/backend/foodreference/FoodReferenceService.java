package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.BusinessException;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class FoodReferenceService {
    private final FoodReferenceRepository foods;
    public FoodReferenceService(FoodReferenceRepository foods) {this.foods=foods;}
    public FoodSearchResponse searchActive(String query,int page,int size) {
        if(page<0) throw BusinessException.invalid("page","Choose a nonnegative page.");
        if(size<1||size>100) throw BusinessException.invalid("size","Choose a page size from 1 to 100.");
        if(query!=null&&query.length()>200) throw BusinessException.invalid("query","Use at most 200 characters.");
        requireCatalogue();
        if(query==null||query.isBlank()) return new FoodSearchResponse(List.of(),0,page,size,false);
        String[] words=query.strip().toLowerCase(Locale.ROOT).split("\\s+");
        Specification<FoodReference> spec=(root,criteria,cb)->{
            List<Predicate> p=new ArrayList<>();p.add(cb.isTrue(root.get("active")));p.add(cb.isTrue(root.join("dataset").get("verified")));
            for(String field:List.of("energyKj100g","proteinG100g","fatG100g","carbsG100g")) p.add(cb.isNotNull(root.get(field)));
            for(String word:words) p.add(cb.like(cb.lower(root.get("displayName")),"%"+escape(word)+"%",'\\'));
            return cb.and(p.toArray(Predicate[]::new));
        };
        var matches=foods.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Order.asc("displayName").ignoreCase(),Sort.Order.asc("id"))));
        return new FoodSearchResponse(matches.getContent().stream().map(FoodReferenceResponse::from).toList(),matches.getTotalElements(),page,size,matches.hasNext());
    }
    public FoodReferenceResponse getImmutableReference(Long id) {return FoodReferenceResponse.from(requireImmutable(id));}
    public FoodReference requireImmutable(Long id) {
        if(id==null||id<1) throw BusinessException.invalid("id","Choose a valid reference identity.");
        return foods.findById(id).orElseThrow(FoodReferenceService::missing);
    }
    public FoodReferenceResponse getActiveByPublicKey(String key) {return FoodReferenceResponse.from(requireActiveByPublicKey(key));}
    public FoodReference requireActiveByPublicKey(String key) {
        if(key==null||key.isBlank()||key.length()>255) throw BusinessException.invalid("publicFoodKey","Choose a valid food key.");
        return foods.findActiveByPublicFoodKey(key).orElseThrow(FoodReferenceService::missing);
    }
    private void requireCatalogue() {if(foods.countSelectable()==0) throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"CATALOGUE_UNAVAILABLE","A verified complete food catalogue has not been configured.");}
    private static BusinessException missing() {return new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Food reference not found.");}
    private static String escape(String value) {return value.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
}
