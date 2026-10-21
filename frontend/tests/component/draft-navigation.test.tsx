import {requireStandaloneFoods as foodRows} from '../../src/features/entry/draftSelectors'
import { describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, Link, RouterProvider } from 'react-router-dom'
import { DraftProvider } from '../../src/app/DraftProvider'
import { useDraft } from '../../src/hooks/useDraft'
import { createDraft,createFoodRow } from '../../src/features/entry/draftReducer'
import type { FoodDraft } from '../../src/features/entry/draftTypes'
import { UnsavedChangesGuard } from '../../src/components/UnsavedChangesGuard'

describe('F05 provider and data-router navigation', () => {
  it('locks duplicate beginSubmission synchronously before React rerenders', () => {
    const result=vi.fn()
    function Probe() {
      const {state,dispatch,beginSubmission}=useDraft()
      return <><output>{state.submission.status}</output><button onClick={()=>{
        const draft=createDraft('1','food','2026-10-16','MANUAL') as FoodDraft
        draft.entries[0]={...createFoodRow(),referenceId:'101',selectedReference:{referenceId:'101',publicFoodKey:'synthetic',displayName:'Synthetic',active:true,dataset:{id:'1',name:'Synthetic',release:'1',revision:'1',sourceUrl:null,nutrientPolicy:'synthetic',verified:false}},quantityGramsInput:'150',mealType:'LUNCH'}
        dispatch({type:'START_NEW',ownerId:'1',draft})
        const payload={submissionId:'0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd',recordDate:draft.recordDate,zone:'Australia/Sydney',source:draft.source,items:[{clientEntryId:foodRows(draft)[0].clientEntryId,referenceId:'101',quantityGrams:150,mealType:'LUNCH' as const}]}
        result(beginSubmission(payload,1,0),beginSubmission(payload,1,0))
      }}>Confirm twice</button></>
    }
    render(<DraftProvider ownerId="1"><Probe /></DraftProvider>)
    fireEvent.click(screen.getByText('Confirm twice'))
    expect(result).toHaveBeenCalledWith(true,false)
    expect(screen.getByText('saving')).toBeTruthy()
  })
  it('owner changes clear the draft immediately without displaying the prior account', () => {
    function Probe() {
      const {state,dispatch}=useDraft()
      return <><output>{state.draft?.ownerId ?? 'empty'}</output><button onClick={()=>dispatch({type:'START_NEW',ownerId:'1',draft:createDraft('1','food','2026-10-16','MANUAL')})}>Start</button></>
    }
    const view=render(<DraftProvider ownerId="1"><Probe /></DraftProvider>)
    fireEvent.click(screen.getByText('Start'));expect(screen.getByText('1')).toBeTruthy()
    view.rerender(<DraftProvider ownerId="2"><Probe /></DraftProvider>)
    expect(screen.getByText('empty')).toBeTruthy();expect(screen.queryByText('1')).toBeNull()
  })
  it('Back navigation asks before discarding and cancel keeps the editor; confirmed leave reaches History', async () => {
    const discard=vi.fn()
    const router=createMemoryRouter([{path:'/entry',element:<><UnsavedChangesGuard dirty pending={null} entryFlow onDiscard={discard} /><p>Editor</p><Link to="/review">Review</Link></>},{path:'/review',element:<p>Review page</p>},{path:'/history',element:<p>History page</p>}],{initialEntries:['/history','/entry'],initialIndex:1})
    render(<RouterProvider router={router} />)
    const beforeUnload=new Event('beforeunload',{cancelable:true});window.dispatchEvent(beforeUnload)
    expect(beforeUnload.defaultPrevented).toBe(true)
    await act(async()=>{await router.navigate(-1)})
    expect(screen.getByRole('dialog')).toBeTruthy()
    fireEvent.click(screen.getByRole('button',{name:'Keep editing'}))
    expect(screen.getByText('Editor')).toBeTruthy();expect(discard).not.toHaveBeenCalled()
    await act(async()=>{await router.navigate(-1)})
    fireEvent.click(screen.getByRole('button',{name:'Discard and leave'}))
    await waitFor(()=>expect(screen.getByText('History page')).toBeTruthy())
    expect(discard).toHaveBeenCalledOnce()
  })
  it('entry-to-review retains the draft and an explicit unknown-result leave discards local recovery', async () => {
    const discard=vi.fn()
    const router=createMemoryRouter([{path:'/entry',element:<><UnsavedChangesGuard dirty pending={null} entryFlow onDiscard={discard}/><Link to="/review">Review</Link></>},{path:'/review',element:<><UnsavedChangesGuard dirty pending="unknown" entryFlow onDiscard={discard}/><Link to="/history">History</Link><p>Review content</p></>},{path:'/history',element:<p>History content</p>}],{initialEntries:['/entry']})
    render(<RouterProvider router={router}/>);fireEvent.click(screen.getByText('Review'))
    await waitFor(()=>expect(screen.getByText('Review content')).toBeTruthy())
    expect(screen.queryByRole('dialog')).toBeNull()
    fireEvent.click(screen.getByText('History'))
    fireEvent.click(await screen.findByRole('button',{name:'Discard and leave'}))
    await waitFor(()=>expect(screen.getByText('History content')).toBeTruthy())
    expect(discard).toHaveBeenCalledOnce()
  })
})
