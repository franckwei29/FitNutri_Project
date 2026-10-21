import type {ParsedEntry,Transcription} from '../../src/api/contracts'
import {imageRecognition} from './media'
import {exerciseType} from './exercise'
export const transcription:Transcription={transcript:'Synthetic controlled voice transcript only.',notices:['Controlled test fixture, not real speech evidence.'],requestId:'f077c20a-60bc-4ea2-8231-34abcf1da968'}
export function parsedFood():ParsedEntry{return {kind:'FOOD',recordDate:'2020-01-02',dateWasDefaulted:false,items:imageRecognition().candidates.map((item,i)=>({...item,mealType:i===0?'LUNCH':null})),exercise:null,missingFields:['items[0].referenceId','items[1].grams','items[1].mealType'],warnings:['Synthetic controlled extraction only; review all fields.'],requestId:transcription.requestId}}
export function parsedExercise():ParsedEntry{return {kind:'EXERCISE',recordDate:'2019-12-31',dateWasDefaulted:false,items:[],exercise:{proposedName:'Synthetic walking',durationMinutes:null,typeMatches:[exerciseType]},missingFields:['exercise.durationMinutes','exercise.exerciseTypeId'],warnings:['Choose the actual type and minutes.'],requestId:transcription.requestId}}
