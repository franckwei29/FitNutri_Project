export interface OwnedState {schemaVersion:number;ownerId:string;foodRecords:{id:string;clientEntryId:string;recordDate:string;quantityGrams:number;energyKj:number|null;mealType:string;version:number;deleted:boolean}[];exerciseRecords:{id:string;clientEntryId:string;recordDate:string;durationMinutes:number;estimatedKj:number|null;estimateStatus:string;version:number;deleted:boolean}[];receipts:{submissionId:string;kind:string;requestSha256:string;records:{id:string;clientEntryId:string;recordDate:string;versionAtCreation:number}[]}[];weights:{recordDate:string;weightKg:number;version:number}[];profile:{onboardingCompleted:boolean;qaConsentGranted:boolean;goalType:string|null;dailyTargetKj:number|null}}
export function syntheticIdentity(prefix?:string):{email:string;password:string;displayName:string}
export function assertE2EEnvironment():{database:string;container:string;user:string}
export function inspectOwnedSyntheticData(email:string):OwnedState

export function acquireSyntheticOwnerLock(email:string):Promise<{pid:number;release:()=>Promise<void>}>
export function assertOriginalPostIsWaiting(lockPid:number):void
export function inspectSyntheticAccountAccess(email:string):{id:string;role:'USER'|'ADMIN';enabled:boolean;version:number}
