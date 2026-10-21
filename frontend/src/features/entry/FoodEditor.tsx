import {FoodComposer} from './food/FoodComposer'
import type {FoodComposerProps} from './food/FoodComposer'
import {standaloneFoods} from './draftSelectors'
import {useCallback,useState} from 'react'
import {Button} from '@mui/material'
import {Add,ArrowForward} from '@mui/icons-material'
import type {EnergyUnit} from '../../api/contracts'
import type {FoodDraft,DraftAction} from './draftTypes'
import type {CatalogueScope} from '../../hooks/useFoodSearch'
import {createFoodRow} from './draftReducer'
import {validateDraft} from './draftValidation'
import {FoodItemEditor} from './FoodItemEditor'
import {foodInputSignature} from './previewIdentity'
import type {RowPreview} from './FoodItemEditor'
import {DateField} from '../../components/presentation'
import {todayInZone} from '../../domain/dates'
import {foodCalculationChanged,isCorrectionDirty} from './correctionIntent'
function ReviewedFoodRows({draft,scope,dispatch,unit,disabled=false,onReview}:{draft:FoodDraft;scope:CatalogueScope;dispatch:(action:DraftAction)=>unknown;unit:EnergyUnit;disabled?:boolean;onReview:()=>void}){
  const [previews,setPreviews]=useState<Record<string,RowPreview>>({})
  const report=useCallback((rowKey:string,value:RowPreview)=>setPreviews(previous=>previous[rowKey]?.signature===value.signature&&previous[rowKey]?.preview===value.preview?previous:{...previous,[rowKey]:value}),[])
  const today=todayInZone(),errors=validateDraft(draft,scope.ownerId,today)
  const original=draft.edit?.original,originalFood=original&&'nutrients' in original?original:undefined,preserved=!!originalFood&&!foodCalculationChanged(draft)
  const ready=errors.length===0&&(!draft.edit||isCorrectionDirty(draft))&&(preserved||standaloneFoods(draft).every(row=>previews[row.rowKey]?.signature===foodInputSignature(row,scope)&&previews[row.rowKey]?.preview!==null&&previews[row.rowKey]?.preview!==undefined))
  return <div className="stack food-editor">
    <fieldset disabled={disabled} style={{border:0,padding:0,margin:0}}><DateField today={today} value={draft.recordDate} onChange={value=>dispatch({type:'CHANGE_DATE',ownerId:scope.ownerId,value})} error={errors.find(e=>e.field==='recordDate')?.message} /></fieldset>
    {standaloneFoods(draft).map((row,index)=><FoodItemEditor key={row.rowKey} row={row} index={index} scope={scope} dispatch={dispatch} unit={unit} disabled={disabled} recordId={draft.edit?.recordId} original={originalFood} onPreview={report} />)}
    {!draft.edit&&<Button variant="outlined" startIcon={<Add />} disabled={disabled||standaloneFoods(draft).length>=50} onClick={()=>dispatch({type:'ADD_FOOD',ownerId:scope.ownerId,row:createFoodRow()})}>{standaloneFoods(draft).length?'Add another food':'Add a food'}</Button>}
    {!ready&&<p role="status">{draft.edit&&!isCorrectionDirty(draft)?'No changes to review. Update the saved record’s date or details.':'Select each food and meal, enter positive grams, then review the current nutrition.'}</p>}
    <div className="actions"><Button variant="contained" endIcon={<ArrowForward />} disabled={disabled||!ready} onClick={onReview}>{draft.edit?'Review food changes':'Review food entries'}</Button></div>
  </div>
}

export function FoodEditor(props:FoodComposerProps){return !props.draft.edit&&props.draft.source==='MANUAL'?<FoodComposer {...props} />:<ReviewedFoodRows {...props} />}
