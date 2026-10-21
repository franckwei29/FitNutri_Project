import type {Ref} from 'react'
import {TextField,Button,Alert} from '@mui/material'
import type {DraftAction,DraftPeriod} from '../draftTypes'
import {parseQuantity} from '../../../domain/inputRules'
import {EquipmentDetailsEditor} from './EquipmentDetailsEditor'
export function ActivityPeriodEditor({period,ownerId,dispatch,disabled=false,durationRef}:{period:DraftPeriod;ownerId:string;dispatch:(action:DraftAction)=>unknown;disabled?:boolean;durationRef?:Ref<HTMLInputElement>}){
 const quantity=parseQuantity(period.durationMinutesInput,'minutes'),option=period.selection.kind==='OPTION'?period.selection.option:null,hint=period.reuseDetails
 const hasHint=!!hint&&(Object.values(hint.equipmentDetails).some(value=>value?.trim())||hint.movements.some(m=>m.setCountInput.trim()||m.repetitionsInput.trim()||m.loadKgInput.trim()))
 const hasDetails=Object.values(period.equipmentDetails).some(value=>value?.trim())||period.movements.some(m=>m.setCountInput.trim()||m.repetitionsInput.trim()||m.loadKgInput.trim())
 return <div className="stack"><TextField inputRef={durationRef} label="Duration (minutes)" value={period.durationMinutesInput} disabled={disabled} onChange={e=>dispatch({type:'SET_PERIOD_DURATION',ownerId,periodId:period.clientEntryId,value:e.target.value})} error={!!period.durationMinutesInput&&!quantity.ok} helperText={period.durationMinutesInput&&!quantity.ok?quantity.message:'Enter this activity period’s minutes, up to 1,440.'} slotProps={{htmlInput:{inputMode:'decimal'}}} />
 {period.lastOptionNameHint&&!option&&<Alert severity="info">{period.lastOptionNameHint} is from saved history. Choose a current activity for this entry.</Alert>}
 {period.lastDurationHint&&<Button disabled={disabled||period.durationMinutesInput.trim()!==''} onClick={()=>dispatch({type:'USE_LAST_DURATION',ownerId,periodId:period.clientEntryId})}>{period.durationMinutesInput.trim()?'Fill empty from last duration':`Use last duration (${period.lastDurationHint} min)`}</Button>}
 {hasHint&&hint&&<Button disabled={disabled||!option||option.id!==hint.optionId} onClick={()=>dispatch({type:'USE_LAST_EXERCISE_DETAILS',ownerId,periodId:period.clientEntryId})}>{hasDetails?'Fill empty from last details':'Use last details'}</Button>}
 {option&&<EquipmentDetailsEditor period={period} option={option} ownerId={ownerId} dispatch={dispatch} disabled={disabled} />}</div>
}
