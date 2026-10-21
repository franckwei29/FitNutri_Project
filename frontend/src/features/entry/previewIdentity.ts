import type {DraftFood} from './draftTypes'
import type {CatalogueScope} from '../../hooks/useFoodSearch'
import type {FoodRecord} from '../../api/contracts'
import {parseQuantity} from '../../domain/inputRules'
export function foodInputSignature(row:DraftFood,scope:CatalogueScope){return JSON.stringify([scope.ownerId,scope.epoch,scope.catalogueRevision,row.referenceId,row.quantityGramsInput])}
export function retainsFoodSnapshot(row:DraftFood,original:FoodRecord){const grams=parseQuantity(row.quantityGramsInput,'grams');return row.clientEntryId===original.clientEntryId&&row.referenceId===(original.reference?.referenceId??null)&&grams.ok&&grams.value===original.quantityGrams}
