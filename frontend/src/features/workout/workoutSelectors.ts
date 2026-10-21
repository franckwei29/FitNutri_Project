export {exercisePeriods,exerciseDurationInput,legacyExerciseId} from '../entry/draftSelectors'

import type {ExerciseDraft,WorkoutDraft} from '../entry/draftTypes'
import {exercisePeriods} from '../entry/draftSelectors'
import {DEFAULT_WORKOUT_TITLE} from '../entry/draftDefaults'
import {parseQuantity} from '../../domain/inputRules'
/** Display only elapsed period time; movements never contribute additional minutes. */
export function workoutMinutes(draft:WorkoutDraft):number|null{if(!draft.periods.length)return null;let cents=0;for(const period of draft.periods){const parsed=parseQuantity(period.durationMinutesInput,'minutes');if(!parsed.ok)return null;cents+=Math.round(parsed.value*100)}return cents/100}
export function exerciseHasInput(draft:ExerciseDraft):boolean{if(draft.voiceInputStale||draft.suggestion)return true;if(draft.entryKind==='WORKOUT'&&(!!draft.titleInput.trim()&&draft.titleInput.trim()!==DEFAULT_WORKOUT_TITLE||draft.periods.length!==1))return true;return exercisePeriods(draft).some(p=>(p.selection.kind==='OPTION'?!!p.selection.option:!!p.selection.referenceId)||!!p.durationMinutesInput.trim()||Object.values(p.equipmentDetails).some(v=>v?.trim())||p.movements.length>0)}
