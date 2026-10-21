import {useFoodRootPreview} from '../../../hooks/useFoodRootPreview'
import {NutrientTiles} from '../../../components/NutrientTiles'
import {Button} from '@mui/material'
import type {DishEditorProps} from './DishEditor'
import {DishEditor} from './DishEditor'
import {portionLabel} from '../../food/portionInput'
export function DishSummaryCard(props:DishEditorProps&{index:number}){const {dish,index,scope,dispatch,disabled}=props;const preview=useFoodRootPreview(dish,scope,props.recordDate,props.source,false,!props.useBatchPreview),current=props.useBatchPreview?props.batchRoot:preview.root;return <section className="food-edit-card" data-root-key={dish.rowKey} aria-label={`Dish ${index+1}`}><h3>{dish.nameInput||'My dish'}</h3><p>{dish.ingredients.length} ingredients · {portionLabel(dish.portion)}</p>{current&&<NutrientTiles values={current.totals} unit={props.unit} />}<details><summary>Edit dish {index+1}</summary><DishEditor {...props} /></details><Button disabled={disabled} onClick={()=>dispatch({type:'REMOVE_ROOT',ownerId:scope.ownerId,rowKey:dish.rowKey})}>Remove dish {index+1}</Button></section>}
