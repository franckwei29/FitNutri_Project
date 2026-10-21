import { useContext } from 'react'
import { DraftContext } from '../app/DraftContext'
export function useDraft() {
  const value = useContext(DraftContext)
  if (!value) throw new Error('DraftProvider is required.')
  return value
}
