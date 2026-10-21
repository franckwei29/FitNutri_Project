package com.fitnutri.backend.records;

import com.fitnutri.backend.activity.*;
import com.fitnutri.backend.common.BusinessException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Repository adapter only. Source conditions/METs remain immutable; names never stand in for IDs. */
@Component @Transactional(readOnly=true)
public class ExerciseCatalog {
    private final ExerciseTypeRepository types;
    public ExerciseCatalog(ExerciseTypeRepository types) {this.types=types;}
    public List<ExerciseTypeResponse> listActiveTypes() {
        var selected=types.findByActiveTrueAndDatasetVerifiedTrue();
        if(selected.isEmpty())throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"CATALOGUE_UNAVAILABLE","A verified activity catalogue has not been configured.");
        return selected.stream().sorted(Comparator.comparing(ExerciseType::getDisplayName).thenComparing(ExerciseType::getId)).map(ExerciseTypeResponse::from).toList();
    }
    public ExerciseType requireImmutable(Long id) {if(id==null||id<1)throw BusinessException.invalid("exerciseTypeId","Choose a valid activity identity.");return types.findById(id).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Activity reference not found."));}
}
