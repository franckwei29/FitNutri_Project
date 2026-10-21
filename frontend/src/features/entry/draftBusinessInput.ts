import type {Draft} from './draftTypes'
import {composerHasInput} from './draftSelectors'
import {exerciseHasInput} from '../workout/workoutSelectors'
import {isCorrectionDirty} from './correctionIntent'
export function hasBusinessDraft(draft:Draft|null){if(!draft)return false;if(draft.edit)return isCorrectionDirty(draft);return draft.kind==='food'?draft.entries.length>0||composerHasInput(draft.composer):exerciseHasInput(draft)}
