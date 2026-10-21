package com.fitnutri.backend.profile;

import com.fitnutri.backend.common.BusinessException;
import org.springframework.http.HttpStatus;

/** Carries only the authenticated owner's conflicting measurement, never an arbitrary supplied ID. */
public class WeightConflictException extends BusinessException {
    private final WeightEntryResponse currentItem;
    public WeightConflictException(WeightEntry current) {
        super(HttpStatus.CONFLICT,"VERSION_CONFLICT","Reload and review the current measurement before correcting it.");
        currentItem=current==null?null:WeightEntryResponse.from(current);
    }
    public WeightEntryResponse currentItem() {return currentItem;}
}
