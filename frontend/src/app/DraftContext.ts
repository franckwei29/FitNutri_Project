import { createContext } from 'react'
import type { CreatePayload, DraftAction, DraftState } from '../features/entry/draftTypes'
import type {ExerciseEstimate} from '../api/contracts'
export interface DraftContextValue {
  state: DraftState
  getSnapshot: () => DraftState
  dispatch: (action: DraftAction) => boolean
  beginSubmission: (payload: CreatePayload, ownerEpoch: number, expectedRevision: number,reviewedEstimate?:ExerciseEstimate) => boolean
}
export const DraftContext = createContext<DraftContextValue | null>(null)
