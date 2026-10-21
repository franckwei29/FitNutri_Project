import type {Page} from '@playwright/test'
import type {JourneyHooks} from './browser-journeys.mjs'
export function setGoalAndReference(page:Page,hooks?:JourneyHooks):Promise<void>
export function switchEnergyDisplayUnit(page:Page,hooks?:JourneyHooks):Promise<void>
export function clearGoalAndReference(page:Page,hooks?:JourneyHooks):Promise<void>
import type {SyntheticIdentity} from './browser-journeys.mjs'
export function correctDatedWeight(page:Page,hooks?:JourneyHooks):Promise<void>
export function confirmWeightCorrection(page:Page,hooks?:JourneyHooks):Promise<void>
export function backfillEarlierWeight(page:Page,hooks?:JourneyHooks):Promise<void>
export function loginAdmin(page:Page,identity:Pick<SyntheticIdentity,'email'|'password'>,hooks?:JourneyHooks):Promise<void>
export function searchManagedAccount(page:Page,email:string,hooks?:JourneyHooks):Promise<void>
export function confirmAccountStatus(page:Page,displayName:string,enabled:boolean,hooks?:JourneyHooks):Promise<void>
