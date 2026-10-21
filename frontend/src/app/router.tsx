import {lazy,Suspense} from 'react'
import type {ComponentType} from 'react'
import type {RouteObject} from 'react-router-dom'
import {AppLayout} from '../layouts/AppLayout'
import {ProtectedRoute} from '../layouts/ProtectedRoute'
import {Home,LoginRoute} from './RouteEntries'
import {RouteLoading,RouteFailure} from './RouteStatus'
/** Called once when constructing routes; lazy modules mount only inside the role guard. */
function page(load:()=>Promise<ComponentType>){const Component=lazy(async()=>({default:await load()}));return <Suspense fallback={<RouteLoading />}><Component /></Suspense>}
export const routes:RouteObject[]=[{path:'/login',element:<LoginRoute />,errorElement:<RouteFailure />},{element:<AppLayout />,errorElement:<RouteFailure />,children:[
  {path:'/',element:<Home />},
  {path:'/onboarding',element:<ProtectedRoute mode="setup">{page(()=>import('../pages/OnboardingPage').then(m=>m.OnboardingPage))}</ProtectedRoute>},
  {path:'/profile',element:<ProtectedRoute mode="profile">{page(()=>import('../pages/ProfilePage').then(m=>m.ProfilePage))}</ProtectedRoute>},
  {path:'/add',element:<ProtectedRoute>{page(()=>import('../pages/AddRecordPage').then(m=>m.AddRecordPage))}</ProtectedRoute>},
  {path:'/entry',element:<ProtectedRoute>{page(()=>import('../pages/EntryPage').then(m=>m.EntryPage))}</ProtectedRoute>},
  {path:'/review',element:<ProtectedRoute>{page(()=>import('../features/entry/ReviewPage').then(m=>m.ReviewPage))}</ProtectedRoute>},
  {path:'/dashboard',element:<ProtectedRoute>{page(()=>import('../pages/DashboardPage').then(m=>m.DashboardPage))}</ProtectedRoute>},
  {path:'/history',element:<ProtectedRoute>{page(()=>import('../pages/HistoryPage').then(m=>m.HistoryPage))}</ProtectedRoute>},
  {path:'/chat',element:<ProtectedRoute>{page(()=>import('../pages/ChatPage').then(m=>m.ChatPage))}</ProtectedRoute>},
  {path:'/admin',element:<ProtectedRoute role="ADMIN">{page(()=>import('../pages/AdminPage').then(m=>m.AdminPage))}</ProtectedRoute>},
  {path:'*',element:<Home />},
]}]
