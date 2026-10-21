import type {ReferenceInfo} from '../../api/contracts'
import type {CatalogueScope} from '../../hooks/useFoodSearch'
import {SmartFoodSearch} from './food/SmartFoodSearch'
import type {FoodSelectionMetadata} from './food/SmartFoodSearch'
/** Shared presentation for manual, ingredient and reviewed media reference selection. */
export function FoodReferencePicker(props:{scope:CatalogueScope;value:ReferenceInfo|null;onSelect:(reference:ReferenceInfo|null,metadata?:FoodSelectionMetadata)=>void;label?:string;disabled?:boolean;selectionTarget?:string;gramsInput?:string;onUseLastGrams?:(value:string)=>void;shortName?:string}){return <SmartFoodSearch {...props} />}
