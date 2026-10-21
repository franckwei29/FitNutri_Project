import type {Page} from '@playwright/test'
import type {JourneyHooks} from './browser-journeys.mjs'
export function reviewFoodCorrection(page:Page,input:{id:string;date:string;grams:number},hooks?:JourneyHooks):Promise<void>
export function confirmCorrection(page:Page,hooks?:JourneyHooks):Promise<void>
export function assertVersionConflict(page:Page,hooks?:JourneyHooks,versions?:{reviewed:number;latest:number}):Promise<{caseId:string;conflict:boolean;reviewedVersion:number;latestVersion:number}>
export function discardCorrection(page:Page,hooks?:JourneyHooks):Promise<void>
export function reviewExerciseDateCorrection(page:Page,input:{id:string;from:string;to:string},hooks?:JourneyHooks):Promise<void>
export function openRecordDeletion(page:Page,input:{id:string;date:string;kind:'food'|'exercise'},hooks?:JourneyHooks):Promise<void>
export function cancelDeletion(page:Page,hooks?:JourneyHooks):Promise<void>
export function confirmDeletion(page:Page,hooks?:JourneyHooks):Promise<void>
