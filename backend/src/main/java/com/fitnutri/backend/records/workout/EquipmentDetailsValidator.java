package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.activity.catalogue.*;
import com.fitnutri.backend.common.BusinessException;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class EquipmentDetailsValidator {
 public Map<String,Object> normalise(ExerciseOption option,Map<String,Object> details){var normalised=ExerciseEntryInputValidator.validateEquipment(details);if(!new HashSet<>(option.getAllowedDetails()).containsAll(normalised.keySet()))throw BusinessException.invalid("equipmentDetails","Remove details that do not apply to this equipment option.");return normalised;}
 public void movement(ExerciseOption option,MovementCatalogue catalogue,ExerciseEntryInputs.Movement input){movement(option,catalogue,input,false);}
 void movement(ExerciseOption option,MovementCatalogue catalogue,ExerciseEntryInputs.Movement input,boolean ownedOriginal){if(catalogue==null)throw BusinessException.invalid("movements","Choose an existing reviewed movement.");if((!catalogue.isActive()&&!ownedOriginal)||!"REVIEWED".equals(catalogue.getReviewStatus()))throw new BusinessException(org.springframework.http.HttpStatus.CONFLICT,"REFERENCE_INACTIVE","This movement is no longer selectable. Choose an active reviewed movement.");if(!"STRENGTH".equals(option.getPeriodKind())||catalogue.getDefaultPeriodOption()==null||!option.getId().equals(catalogue.getDefaultPeriodOption().getId()))throw BusinessException.invalid("movements","Use this movement only with its reviewed strength-period option.");var allowed=catalogue.getAllowedFields();if(input.setCount()!=null&&!allowed.contains("setCount")||input.repetitions()!=null&&!allowed.contains("repetitions")||input.loadKg()!=null&&!allowed.contains("loadKg"))throw BusinessException.invalid("movements","Use only this movement's optional detail fields.");}
}
