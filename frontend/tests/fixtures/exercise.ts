import {createDraft} from '../../src/features/entry/draftReducer'
import type {ExerciseEstimate,ExerciseType} from '../../src/api/contracts'
export const exerciseType:ExerciseType={id:'9007199254740993',activityKey:'walking',displayName:'Walking for pleasure',sourceActivityCode:'17160',sourceDescription:'walking for pleasure',metValue:3.5,active:true,dataset:{id:'2',name:'Adult Compendium',release:'2024',revision:'r1',sourceUrl:'https://pacompendium.com/',nutrientPolicy:'MET_KG_MIN_KJ_V1',verified:true,minAge:19,maxAge:59}}
export function exerciseEstimate(overrides:Partial<ExerciseEstimate>={}):ExerciseEstimate{return {exerciseType,recordDate:'2026-10-16',durationMinutes:10.5,estimatedKj:183.562,estimateStatus:'AVAILABLE',unavailableReason:null,weightKgUsed:71.5,weightDateUsed:'2026-10-15',metUsed:3.5,formulaVersion:'MET_KG_MIN_KJ_V1',calculationFingerprint:'a'.repeat(64),...overrides}}

/** Explicit V1 reference adapter over the sole new Quick period state. */
export function legacyExerciseDraft(ownerId='1',minutes='12.5'){
 const draft=createDraft(ownerId,'exercise','2026-10-16','MANUAL')
 if(draft.kind!=='exercise'||draft.entryKind!=='QUICK')throw new Error('Expected Quick fixture')
 return {...draft,period:{...draft.period,selection:{kind:'REFERENCE' as const,referenceId:exerciseType.id},durationMinutesInput:minutes}}
}
