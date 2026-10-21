import { useContext } from 'react'
import { SessionContext } from '../app/SessionContext'
export function useSession() {const value=useContext(SessionContext);if(!value) throw Error('SessionProvider is required.');return value}
