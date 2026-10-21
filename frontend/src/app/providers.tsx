import type { ReactNode } from 'react'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClientProvider } from '@tanstack/react-query'
import { createQueryClient } from './queryClient'
import { SessionProvider } from './SessionProvider'
import { theme } from './theme'

const queryClient = createQueryClient()
export function AppProviders({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={queryClient}><ThemeProvider theme={theme}><CssBaseline /><SessionProvider>{children}</SessionProvider></ThemeProvider></QueryClientProvider>
}
