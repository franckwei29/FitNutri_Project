import type {Page} from '@playwright/test'
export interface JourneyHooks {baseURL?:string;waitOptions?:{timeoutMs:number};afterStep?:()=>Promise<unknown>;setDate?:(label:string,value:string)=>Promise<unknown>}
export interface SyntheticIdentity {email:string;password:string;displayName:string}
export function registerAndLogin(page:Page,identity:SyntheticIdentity,hooks?:JourneyHooks):Promise<{caseId:string;registered:boolean;onboardingVisible:boolean}>
export function completeQuestionnaire(page:Page,hooks?:JourneyHooks):Promise<{caseId:string;qaConsent:boolean;goal:null;weightDate:string}>
export function beginManualFood(page:Page,hooks?:JourneyHooks):Promise<void>
export function fillFood(page:Page,number:number,input:{query:string;name:string;referenceCode:string;grams:number;meal:string},hooks?:JourneyHooks):Promise<void>
export function enterTwoFoodsForReview(page:Page,hooks?:JourneyHooks):Promise<{caseId:string;count:number;recordDate:string}>
export function confirmToUnknown(page:Page,hooks?:JourneyHooks):Promise<{caseId:string;submissionId:string;unknown:boolean}>
export function assertFrozenNavigation(page:Page,submissionId:string,hooks?:JourneyHooks):Promise<{caseId:string;frozen:boolean}>
export function recoverOriginalReceipt(page:Page,submissionId:string,hooks?:JourneyHooks):Promise<{caseId:string;submissionId:string;recovered:boolean}>
