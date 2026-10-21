import type {Capabilities} from './contracts'
import {CapabilitiesSchema} from './schemas'
import {apiGet} from './client'
import {decodeResponse} from './validators'
export async function fetchCapabilities(signal?:AbortSignal):Promise<Capabilities>{return decodeResponse(CapabilitiesSchema,await apiGet('/capabilities',{signal}))}
