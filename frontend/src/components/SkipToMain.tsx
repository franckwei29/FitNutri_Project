/** Move keyboard focus without creating a hash entry outside the router's navigation guard. */
export function SkipToMain(){return <a className="skip-link" href="#main-content" onClick={event=>{event.preventDefault();document.getElementById('main-content')?.focus()}}>Skip to main content</a>}
