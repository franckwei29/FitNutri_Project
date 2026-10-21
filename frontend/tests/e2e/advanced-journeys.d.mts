import type {Page} from '@playwright/test'
export function verifyUnavailableHelpers(page:Page,hooks?:{baseURL?:string;afterStep?:()=>Promise<unknown>}):Promise<{caseId:string;serverUnconfigured:boolean;qaConsentRestricted:boolean;manualRecordingAccessible:boolean}>
