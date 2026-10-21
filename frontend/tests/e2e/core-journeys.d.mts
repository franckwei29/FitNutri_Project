import type {Page} from '@playwright/test'
import type {JourneyHooks,SyntheticIdentity} from './browser-journeys.mjs'
export function cancelFoodReview(page:Page,hooks?:JourneyHooks):Promise<{caseId:string;cancelled:boolean}>
export function confirmFoodWithDoubleClick(page:Page,hooks?:JourneyHooks):Promise<{caseId:string;saved:boolean;count:number}>
export function enterMissingWeightExerciseReview(page:Page,hooks?:JourneyHooks):Promise<{caseId:string;minutes:number;date:string}>
export function confirmExercise(page:Page,hooks?:JourneyHooks):Promise<{caseId:string;saved:boolean}>
export function logout(page:Page,hooks?:JourneyHooks):Promise<void>
export function loginExisting(page:Page,identity:SyntheticIdentity,hooks?:JourneyHooks):Promise<void>
