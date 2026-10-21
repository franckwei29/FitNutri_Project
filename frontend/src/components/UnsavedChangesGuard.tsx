import { ConfirmDialog } from './presentation'
import { useUnsavedChanges } from '../hooks/useUnsavedChanges'
import type { PendingSubmission } from '../hooks/useUnsavedChanges'

export function UnsavedChangesGuard({ dirty, pending, entryFlow = false,editFlow=false,settings = false, recordChange=false,onDiscard }: {
  dirty: boolean; pending: PendingSubmission; entryFlow?: boolean;editFlow?:boolean; settings?: boolean;recordChange?:boolean; onDiscard: () => void|boolean
}) {
  const blocker = useUnsavedChanges(dirty, pending, entryFlow,editFlow)
  return <ConfirmDialog open={blocker.state === 'blocked'} title={pending ? 'Confirmation is still pending' : 'Discard unsaved changes?'}
    cancelLabel={pending?'Stay on this page':settings?'Cancel':'Keep editing'} busy={pending === 'saving'} label={pending === 'unknown' ? (settings?'Leave and check the saved state later':'Discard and leave') : 'Discard and leave'}
    onCancel={() => blocker.reset?.()} onConfirm={() => {
      if (pending === 'saving') return
      if ((pending !== 'unknown'||!settings)&&onDiscard()===false)return
      blocker.proceed?.()
    }}>
    {pending === 'saving' ? 'Wait for the server confirmation.' : pending === 'unknown' ? (settings&&!recordChange?'Your change may already be saved. Check its current saved state before repeating it. Leaving does not cancel the server operation.':"The save may already have completed. Leaving won't cancel it. Check History before recording it again.") : 'Your unsubmitted changes will be discarded.'}
  </ConfirmDialog>
}
