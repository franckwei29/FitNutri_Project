import type {FoodCandidate} from '../../../api/contracts'
import {createFoodRow} from '../draftReducer'
import type {DraftFood} from '../draftTypes'
/** Candidate identities never become record/client identities; all references/meals remain unselected. */
export function mapImageCandidates(candidates:FoodCandidate[]):DraftFood[] {
  return candidates.map(candidate=>({...createFoodRow(),quantityGramsInput:candidate.suggestedGrams===null?'':String(candidate.suggestedGrams),suggestion:{origin:'IMAGE',candidateKey:candidate.candidateKey,proposedName:candidate.proposedName,referenceMatches:structuredClone(candidate.referenceMatches),warnings:[...candidate.warnings]}}))
}
