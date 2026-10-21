# F03/F04 — server sessions and Demo authentication screens

Branch develop/yuankang-wei; no backend or database change in this package. Peer review and UAT pending.

Added SessionProvider/Context, sessionEpoch controller, safe sessionRouting, owner queryKeys, configured queryClient, useSession, ProtectedRoute, SessionBoundary, data router/route components and Header. Replaced AppLayout routing and old LoginPage scaffold with AuthPage adapted from Demo account.tsx (same brand/story/form/mode/password visibility/layout). Auth API now accepts AbortSignal for login. Added canonical profile API boundaries for dependent F06. Existing DraftProvider is keyed by authenticated owner/session epoch and mounted once; confirmed logout/expiry clears queries and draft state. Header warns before abandoning an existing draft and blocks logout while a save is pending. No LocalStorage/SessionStorage identity or business data, demo account prefill or mock passwords remain in application source.

me and profile failures are distinct from anonymous state; profile500 renders retry, never onboarding. USER/ADMIN/setup/adult-reconfirmation routes use server data. Administrator bootstrap skips private profile. Private query keys include string owner IDs; epoch checks reject late profile responses before either state or cache writes. Wrong credentials keep login form/error; registration201 shows success and requires explicit sign in. Login restores CSRF then me/profile; logout awaits server response and does not claim success on failure. returnTo only accepts allowlisted internal user routes.

Tests: new tests/unit/session.test.ts9 and tests/component/session-auth.test.tsx10 (shared synthetic profile fixture). Existing41 unchanged. Test-first session run failed missing module (/tmp/fitnutri-f03-red.log); initial session9 PASS. Initial build caught erasableSyntaxOnly parameter properties and insufficient discriminated-union narrowing, then an obsolete LoginPage mutation signature; fixed without loosening TypeScript. Initial lint caught mixed component/route exports and control-regex; split route components and used character-code checks without disabling rules. Final actual commands:

```
npm --prefix frontend test
npm --prefix frontend run typecheck:test
npm --prefix frontend run lint
npm --prefix frontend run build
```

Final60 tests PASS, zero failures/skips (/tmp/fitnutri-f03-final-tests.log). Strict app/test types, lint and build PASS (corresponding /tmp/fitnutri-f03-final-*.log). Bundle warning694.00KB retained. git diff --check PASS. Source search found no LocalStorage/SessionStorage/mock store/demo credential references.

Real browser smoke: Vite localhost5173 -> dedicated Spring127.0.0.1:8080 -> synthetic fitnutri_integration_browser_20261016 PG16 only. Restarted our own backend to load B03/B04. Existing synthetic account browser.smoke@example.test logged in through the reused Demo form, fetched canonical empty profile, routed to /onboarding, retained session after refresh, then real logout returned /login. Screenshot screenshots/server-session-login.jpg shows the actual empty signed-out login form. No user credentials or existing business data used. This is identity smoke, not full M1 E2E. Profile/onboarding form is explicitly F06 next; other business routes are placeholders and are not reported implemented. No remote Git operation or shared database modification.
