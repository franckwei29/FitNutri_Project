import {useQuery} from '@tanstack/react-query'
import {fetchRecentFoodV2} from '../api/foodEntriesV2'
import type {CatalogueScope} from './useFoodSearch'
/** Private readonly reuse hints, never an independently editable business-data store. */
export function useRecentFoods(scope:CatalogueScope,enabled=true){return useQuery({queryKey:['owner',scope.ownerId,scope.epoch,'recent-food-roots',scope.catalogueRevision],queryFn:({signal})=>fetchRecentFoodV2(signal),enabled,retry:false})}
