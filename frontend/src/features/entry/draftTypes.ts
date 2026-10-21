import type {RootMutationAction} from './rootMutationTypes'
import type { ApiError, EntrySource, ExerciseCreateRequest, FoodBatchRequest, Id, MealType, ReferenceInfo, SubmissionReceipt,FoodRecord,ExerciseRecord,ExerciseEstimate,ExerciseType,ExerciseOptionV2,MovementCatalogueV2,SelectionKindV2 } from '../../api/contracts'
import type {EntrySubmissionV2} from '../../api/submissionsV2'
import type {FoodPreviewV2,ExercisePreviewV2,SubmissionReceiptV2} from '../../api/contracts'
import type {FoodRootV2,ExerciseRootV2} from '../../api/contracts'
import type {RootKind} from '../history/historySearch'
import type {RecordMutationState,RecordMutationAction} from './recordMutationTypes'

export interface FoodSuggestion {origin: 'IMAGE'|'VOICE'; candidateKey:string; proposedName:string; referenceMatches:ReferenceInfo[]; warnings:string[]}
export interface DraftFood {
  entryKind: 'SINGLE'; selectionKind?:SelectionKindV2; searchMetadataRevision?:string; selectionShortName?:string;
  rowKey: string; clientEntryId: string; referenceId: Id | null; selectedReference: ReferenceInfo | null
  suggestion?:FoodSuggestion
  lastAmountHint?:string;quantityGramsInput: string; mealType: MealType | '';
}
export interface EditTarget { recordId: Id; expectedVersion: number; originalRecordDate: string; original?:FoodRecord|ExerciseRecord;root?:{kind:RootKind;original:FoodRootV2|ExerciseRootV2} }
interface DraftBase {reuseFrom?:{kind:RootKind;id:string};reuseFromName?:string; voiceInputStale?:boolean; ownerId: Id; recordDate: string; source: EntrySource; revision: number; returnTo: string; edit?: EditTarget }
export type DraftIngredient=Omit<DraftFood,'entryKind'|'mealType'>
export type DraftPortion={kind:'PERSONAL'}|{kind:'FRACTION';numeratorInput:string;denominatorInput:string}|{kind:'PERCENT';percentInput:string}|{kind:'SERVINGS';totalServingsInput:string;eatenServingsInput:string}
export interface DraftDish {entryKind:'DISH';rowKey:string;clientEntryId:string;nameInput:string;mealType:MealType|'';inputMode:'PERSONAL'|'FULL_RECIPE';portion:DraftPortion;ingredients:DraftIngredient[];originDishId?:string;reuseAmounts?:Record<string,string>;ingredientQueries?:Record<string,string>}
export type FoodRootDraft=DraftFood|DraftDish
export type FoodSelectionTarget={kind:'SINGLE';rowKey:string}|{kind:'INGREDIENT';parentRowKey:string;rowKey:string}
export interface FoodDraft extends DraftBase { kind: 'food'; entries: FoodRootDraft[];defaultMealType:MealType|'';composer:FoodRootDraft|null;selectionTarget:FoodSelectionTarget|null;composerQuery:string }
export interface ExerciseSuggestion {origin:'VOICE';proposedName:string|null;typeMatches:ExerciseType[];warnings:string[]}
export type VoicePending={kind:'food';recordDate:string;foods:DraftFood[]}|{kind:'exercise';recordDate:string;durationMinutesInput:string;suggestion:ExerciseSuggestion}
export interface DraftMovement {selectableForNew?:boolean;rowKey:string;catalogue:MovementCatalogueV2;setCountInput:string;repetitionsInput:string;loadKgInput:string}
export type ExerciseSelection={kind:'REFERENCE';referenceId:Id|null}|{kind:'OPTION';option:ExerciseOptionV2|null}
export interface DraftPeriod {clientEntryId:string;periodKind:'STRENGTH'|'ACTIVITY';selection:ExerciseSelection;durationMinutesInput:string;lastOptionNameHint?:string;lastDurationHint?:string;reuseDetails?:{optionId:string;equipmentDetails:DraftPeriod['equipmentDetails'];movements:DraftMovement[]};equipmentDetails:Partial<Record<'speedKmh'|'inclinePercent'|'distanceMetres'|'machineLevel',string>>;movements:DraftMovement[]}
export interface QuickExerciseDraft extends DraftBase {kind:'exercise';entryKind:'QUICK';suggestion?:ExerciseSuggestion;clientEntryId:string;period:DraftPeriod}
export interface WorkoutDraft extends DraftBase {kind:'exercise';entryKind:'WORKOUT';suggestion?:ExerciseSuggestion;clientEntryId:string;titleInput:string;periods:DraftPeriod[];originWorkoutId?:string}
export type ExerciseDraft=QuickExerciseDraft|WorkoutDraft
export type Draft = FoodDraft | ExerciseDraft
export type CreatePayload = FoodBatchRequest | ExerciseCreateRequest
interface FrozenSubmission {apiVersion?:1; submissionId: string; frozenPayload: CreatePayload; ownerEpoch: number;reviewedEstimate?:ExerciseEstimate }
interface FrozenV2 {apiVersion:2;envelope:EntrySubmissionV2;submissionId:string;ownerEpoch:number;foodPreview?:FoodPreviewV2;exercisePreview?:ExercisePreviewV2}
export interface LastSavedV2 {noticeStartedAt?:number;receipt:SubmissionReceiptV2;domain:'FOOD'|'EXERCISE';recordDate:string;previousDate?:string;defaultMealType?:MealType|'';noticeId:number;ownerEpoch:number;refreshFailed:boolean}
export type SubmissionState = { status: 'idle' } | ({ status: 'saving' | 'unknown' } & FrozenSubmission) | ({status:'saving'|'unknown'}&FrozenV2) | { status: 'rejected';apiVersion?:1|2; error: ApiError;reviewedEstimate?:ExerciseEstimate;changedPreview?:ExerciseEstimate } | { status: 'saved'; receipt: SubmissionReceipt; refreshFailed: boolean }
export type UndoRemoval=({kind:'FOOD_ROOT';index:number;root:FoodRootDraft}|{kind:'INGREDIENT';index:number;parentRowKey:string;ingredient:DraftIngredient}|{kind:'PERIOD';index:number;period:DraftPeriod}|{kind:'MOVEMENT';index:number;periodId:string;movement:DraftMovement})&{token?:number}
export interface DraftState { ownerId: Id | null; draft: Draft | null; submission: SubmissionState;recordMutation:RecordMutationState;lastSaved?:LastSavedV2;undoRemoval?:UndoRemoval;preferredExerciseMode?:'QUICK'|'WORKOUT';preferredMealType?:MealType|'';noticeSequence?:number }
type Owner = { ownerId: Id;undoToken?:number }
type Row = Owner & { rowKey: string }
export type DraftAction = (Owner&{type:'ABANDON_UNKNOWN';kind:'SUBMISSION'|'RECORD_MUTATION';identity:string;ownerEpoch:number})
  | (Owner&{type:'EXPIRE_UNDO';token:number})
  | (Owner&{type:'END_UNDO_WINDOW'})
  | (Owner&{type:'CLEAR_LAST_SAVED';submissionId:string})
  | RootMutationAction
  | RecordMutationAction
  | (Owner&{type:'BEGIN_SUBMIT_V2';ownerEpoch:number;expectedRevision:number;envelope:EntrySubmissionV2;foodPreview?:FoodPreviewV2;exercisePreview?:ExercisePreviewV2;today:string})
  | (Owner&{type:'SUBMIT_V2_UNKNOWN';ownerEpoch:number;submissionId:string})
  | (Owner&{type:'SUBMIT_V2_REJECTED';ownerEpoch:number;submissionId:string;error:ApiError})
  | (Owner&{type:'SUBMIT_V2_CONFIRMED';ownerEpoch:number;receipt:SubmissionReceiptV2;nextDraft:Draft;today:string;noticeAt?:number})
  | (Owner&{type:'CONTINUE_SAVED_DATE';noticeId:number;confirmed:boolean})
  | (Owner&{type:'V2_REFRESH';noticeId:number;failed:boolean})
  | (Owner&{type:'SET_DEFAULT_MEAL';value:MealType|''})
  | (Owner&{type:'COMPOSER_START';composer:FoodRootDraft;replaceConfirmed?:boolean;expectedRevision?:number;expectedComposerId?:string|null})
  | (Owner&{type:'COMPOSER_SET_QUERY';expectedComposerId:string;value:string})
  | (Owner&{type:'COMPOSER_SET_GRAMS';expectedComposerId:string;value:string;onlyIfEmpty?:boolean})
  | (Owner&{type:'COMPOSER_SELECT_REFERENCE';expectedComposerId:string;reference:ReferenceInfo|null;metadata?:{selectionKind:SelectionKindV2;metadataRevision:string|null;shortName:string}})
  | (Owner&{type:'COMPOSER_UPDATE';expectedComposerId:string;root:FoodRootDraft})
  | (Owner&{type:'COMPOSER_ADD';expectedRevision:number;expectedComposerId:string;nextComposer:DraftFood;today:string})
  | (Owner&{type:'COMPOSER_CANCEL';expectedComposerId:string;nextComposer:DraftFood})
  | (Owner&{type:'BEGIN_REFERENCE_REPLACEMENT';target:FoodSelectionTarget})
  | (Owner&{type:'CANCEL_REFERENCE_REPLACEMENT'})
  | (Owner&{type:'APPLY_REFERENCE';target:FoodSelectionTarget;reference:ReferenceInfo;metadata?:{selectionKind:SelectionKindV2;metadataRevision:string|null;shortName:string}})
  | (Owner&{type:'ADD_DISH';dish:DraftDish})
  | (Row&{type:'REMOVE_ROOT'})
  | (Owner&{type:'UNDO_REMOVAL'})
  | (Row&{type:'SET_DISH_NAME';value:string})
  | (Row&{type:'CHANGE_DISH_MODE';expectedMode:DraftDish['inputMode'];value:DraftDish['inputMode'];confirmed:boolean})
  | (Row&{type:'USE_LAST_DISH_AMOUNTS'})
  | (Row&{type:'SET_INGREDIENT_QUERY';ingredientRowKey:string;value:string})
  | (Row&{type:'SET_DISH_PORTION';inputMode:DraftDish['inputMode'];portion:DraftPortion})
  | (Row&{type:'ADD_INGREDIENT';ingredient:DraftIngredient})
  | (Row&{type:'REMOVE_INGREDIENT';ingredientRowKey:string})
  | (Owner&{type:'SELECT_QUICK_EXERCISE';periodId:string;expectedRevision?:number;option:ExerciseOptionV2;movement?:DraftMovement;clearIncompatibleConfirmed?:boolean;lastDurationHint?:string})
  | (Owner&{type:'USE_LAST_DURATION'|'USE_LAST_EXERCISE_DETAILS';periodId:string})
  | (Owner&{type:'ADD_PERIOD';period:DraftPeriod})
  | (Owner&{type:'REMOVE_PERIOD';periodId:string})
  | (Owner&{type:'SET_WORKOUT_TITLE';value:string})
  | (Owner&{type:'SET_PERIOD_DURATION';periodId:string;value:string})
  | (Owner&{type:'SET_PERIOD_OPTION';periodId:string;option:ExerciseOptionV2;clearIncompatibleConfirmed?:boolean})
  | (Owner&{type:'SET_EQUIPMENT_DETAIL';periodId:string;field:keyof DraftPeriod['equipmentDetails'];value:string})
  | (Owner&{type:'ADD_MOVEMENT';periodId:string;movement:DraftMovement})
  | (Owner&{type:'REMOVE_MOVEMENT';periodId:string;rowKey:string})
  | (Owner&{type:'SET_MOVEMENT_DETAIL';periodId:string;rowKey:string;field:'setCountInput'|'repetitionsInput'|'loadKgInput';value:string})
  | (Owner & { type: 'START_NEW'; draft: Draft; replaceConfirmed?: boolean })
  | (Owner & { type: 'LOAD_EDIT'; draft: Draft })
  | (Owner & { type: 'IMPORT_IMAGE_CANDIDATES'; expectedRevision:number; rows:DraftFood[] })
  | (Owner & { type:'MARK_VOICE_INPUT_STALE';value:boolean })
  | (Owner & { type:'IMPORT_VOICE_CANDIDATES';expectedRevision:number;pending:VoicePending })
  | (Owner & { type: 'USE_MANUAL_ENTRY'; expectedRevision:number;composer?:DraftFood })
  | (Owner & { type: 'ADD_FOOD'; row: DraftFood })
  | (Row & { type: 'REMOVE_FOOD' })
  | (Row & { type: 'SELECT_REFERENCE'; reference: ReferenceInfo;metadata?:{selectionKind:SelectionKindV2;metadataRevision:string|null;shortName:string} })
  | (Row & { type: 'CLEAR_REFERENCE' })
  | (Row & { type: 'CHANGE_GRAMS'; value: string;onlyIfEmpty?:boolean })
  | (Row & { type: 'CHANGE_MEAL'; value: MealType | '' })
  | (Owner & { type: 'CHANGE_DATE'; value: string })
  | (Owner & { type: 'SET_EXERCISE_TYPE'; value: Id | null })
  | (Owner & { type: 'SET_DURATION'; value: string })
  | (Owner & { type: 'BEGIN_SUBMIT'; ownerEpoch: number; expectedRevision: number; payload: CreatePayload;reviewedEstimate?:ExerciseEstimate })
  | (Owner & { type: 'SUBMIT_UNKNOWN'; ownerEpoch: number; submissionId: string })
  | (Owner & { type: 'SUBMIT_REJECTED'; ownerEpoch: number; submissionId: string; error: ApiError;changedPreview?:ExerciseEstimate })
  | (Owner & { type: 'SUBMIT_CONFIRMED'; ownerEpoch: number; receipt: SubmissionReceipt })
  | (Owner & { type: 'REFRESH_FAILED' | 'REFRESH_COMPLETED' })
  | (Owner & { type: 'DISCARD' })
