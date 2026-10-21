import { useContext } from 'react'
import { NavigationSafetyContext } from '../app/NavigationSafetyContext'
export function useNavigationSafety() {return useContext(NavigationSafetyContext)}
