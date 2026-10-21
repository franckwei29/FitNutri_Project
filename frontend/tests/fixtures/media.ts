import {requireStandaloneFoods as foodRows} from '../../src/features/entry/draftSelectors'
import type {ImageRecognition} from '../../src/api/contracts'
import {foodDraft} from './entry'
export function imageRecognition():ImageRecognition{return {requestId:'b3287115-7e09-4f45-a6a6-7fe063d67802',notices:['Synthetic controlled-provider fixture; not a real model result.'],candidates:[{candidateKey:'candidate-one',proposedName:'Synthetic rice suggestion',suggestedGrams:150,referenceMatches:[foodRows(foodDraft())[0].selectedReference!],warnings:['Review the suggested reference and grams.']},{candidateKey:'candidate-two',proposedName:'Synthetic unmatched dish',suggestedGrams:null,referenceMatches:[],warnings:['Search manually.']}]}}
