import { createContext } from 'react'
import type { SessionController,SessionState } from './sessionEpoch'
export const SessionContext=createContext<{state:SessionState;actions:SessionController}|null>(null)
