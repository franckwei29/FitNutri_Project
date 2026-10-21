import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Brand, ConfirmDialog, DateField, ErrorBox, LoadingButton } from '../../src/components/presentation'

describe('F00 reused Demo presentation', () => {
  it('renders the original brand as an internal home link', () => {
    render(<MemoryRouter><Brand /></MemoryRouter>)
    expect(screen.getByRole('link').getAttribute('href')).toBe('/')
    expect(screen.getByRole('link').textContent).toContain('FitNutri')
  })
  it('pending actions cannot submit again', () => {
    const submit=vi.fn()
    render(<LoadingButton busy onClick={submit}>Save</LoadingButton>)
    fireEvent.click(screen.getByRole('button'))
    expect(submit).not.toHaveBeenCalled()
    expect(screen.getByRole('button').getAttribute('disabled')).not.toBeNull()
  })
  it('confirmation is accessible, cancellation performs no confirmation', () => {
    const cancel=vi.fn(), confirm=vi.fn()
    render(<ConfirmDialog open title="Delete this record?" onCancel={cancel} onConfirm={confirm}>This changes the selected record.</ConfirmDialog>)
    expect(screen.getByRole('dialog').getAttribute('aria-labelledby')).toBeTruthy()
    fireEvent.click(screen.getByRole('button',{name:'Cancel'}))
    expect(cancel).toHaveBeenCalledOnce()
    expect(confirm).not.toHaveBeenCalled()
  })
  it('date field uses supplied application today and error region announces failures', () => {
    render(<><DateField value="2026-10-15" today="2026-10-16" onChange={vi.fn()} /><ErrorBox message="Server unavailable" /></>)
    expect(screen.getByLabelText('Record date').getAttribute('max')).toBe('2026-10-16')
    expect(screen.getByRole('alert').textContent).toBe('Server unavailable')
  })
})
