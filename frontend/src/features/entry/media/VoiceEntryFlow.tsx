import {createFoodRow} from '../draftReducer'
import {standaloneFoods} from '../draftSelectors'
import {useState} from 'react'
import type {EnergyUnit} from '../../../api/contracts'
import type {CatalogueScope} from '../../../hooks/useFoodSearch'
import type {Draft,DraftAction} from '../draftTypes'
import {FoodEditor} from '../FoodEditor'
import {ExerciseEntryEditorV2} from '../exercise/ExerciseEntryEditorV2'
import {VoiceEntryPanel} from './VoiceEntryPanel'
import {voiceImportAction} from './voiceDraft'
interface Props {draft:Draft;scope:CatalogueScope;unit:EnergyUnit;profileVersion:number|null;disabled:boolean;transcriptionAvailable:boolean;parsingAvailable:boolean;isCurrent:()=>boolean;dispatch:(action:DraftAction)=>boolean;onReview:()=>void}
export function VoiceEntryFlow({draft,scope,unit,profileVersion,disabled,transcriptionAvailable,parsingAvailable,isCurrent,dispatch,onReview}:Props){
 const [blocked,setBlocked]=useState(!!draft.voiceInputStale),assisted=draft.kind==='food'?standaloneFoods(draft).some(row=>row.suggestion):!!draft.suggestion
 return <div className="stack"><VoiceEntryPanel draft={draft} transcriptionAvailable={!disabled&&transcriptionAvailable} parsingAvailable={!disabled&&parsingAvailable} isCurrent={isCurrent} onAccept={(parsed,revision)=>{if(!isCurrent())return false;const action=voiceImportAction(parsed,scope.ownerId,revision);return !!action&&dispatch(action)}} onManual={()=>{if(isCurrent())dispatch({type:'USE_MANUAL_ENTRY',ownerId:scope.ownerId,expectedRevision:draft.revision,...(draft.kind==='food'?{composer:{...createFoodRow(),mealType:draft.defaultMealType}}:{})})}} onEntryBlocked={value=>{if(isCurrent()){setBlocked(value);dispatch({type:'MARK_VOICE_INPUT_STALE',ownerId:scope.ownerId,value})}}} />
 {assisted&&(draft.kind==='food'?<FoodEditor draft={draft} scope={scope} dispatch={dispatch} unit={unit} disabled={disabled||blocked} onReview={onReview} />:<><p>Voice suggestion: {draft.suggestion?.proposedName}. Choose the matching activity below. No activity is selected automatically.</p><ExerciseEntryEditorV2 showModeControls={false} draft={draft} scope={scope} dispatch={dispatch} unit={unit} profileVersion={profileVersion} disabled={disabled||blocked} onReview={onReview} /></>)}
 </div>
}
