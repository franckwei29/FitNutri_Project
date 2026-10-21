import {useQuery} from '@tanstack/react-query'
import {fetchRecentExerciseV2} from '../api/exerciseEntriesV2'
import type {CatalogueScope} from './useFoodSearch'
export function useRecentExercise(scope:CatalogueScope,enabled=true){return useQuery({queryKey:['owner',scope.ownerId,scope.epoch,'recent-exercise-roots',scope.catalogueRevision],queryFn:({signal})=>fetchRecentExerciseV2(signal),enabled,retry:false})}
