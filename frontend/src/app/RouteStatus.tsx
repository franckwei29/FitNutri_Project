import {Button} from '@mui/material'
export function RouteLoading(){return <p className="route-loading" role="status" aria-live="polite">Loading page…</p>}
export function RouteFailure(){return <main id="main-content" tabIndex={-1} className="page narrow-page"><h1>This page could not be loaded.</h1><p>Check your connection and reload. Saved records remain on the server; unsubmitted changes on this page may be lost.</p><Button component="a" href="/" variant="contained">Reload application</Button></main>}
