import {useEffect} from 'react'
import {Alert} from '@mui/material'
import type {DraftFood} from './draftTypes'
import type {RowPreview} from './FoodItemEditor'
import type {CatalogueScope} from '../../hooks/useFoodSearch'
import type {EnergyUnit,FoodRecord} from '../../api/contracts'
import {useFoodPreview} from '../../hooks/useFoodPreview'
import {foodInputSignature,retainsFoodSnapshot} from './previewIdentity'
import {mealLabels} from '../../domain/labels'
import {NutrientTiles} from '../../components/NutrientTiles'
import {SourceNote} from '../../components/SourceNote'
export function ReviewFoodRow({row,index,scope,unit,onPreview,original,recordId}:{row:DraftFood;index:number;scope:CatalogueScope;unit:EnergyUnit;onPreview:(key:string,value:RowPreview)=>void;original?:FoodRecord;recordId?:string}){
  const preserved=!!original&&retainsFoodSnapshot(row,original)
  const result=useFoodPreview(preserved?null:row.referenceId,row.quantityGramsInput,scope,recordId),current=result.ready?result.data??null:null,signature=foodInputSignature(row,scope)
  useEffect(()=>{onPreview(row.rowKey,{signature,preview:current})},[row.rowKey,signature,current,onPreview])
  return <section className="food-edit-card"><div className="food-edit-heading"><span className="item-number">{String(index+1).padStart(2,'0')}</span><strong>{row.selectedReference?.displayName??'Reference required'}</strong><span className="tag">{row.mealType?mealLabels[row.mealType]:'Meal required'}</span></div><p>{row.quantityGramsInput} g</p>
    {result.isFetching&&<p role="status">Checking current nutrition…</p>}{result.isError&&<Alert severity="error">{result.error instanceof Error?result.error.message:'Nutrition could not be checked.'}</Alert>}
    {current&&<><NutrientTiles values={current.nutrients} unit={unit} /><SourceNote dataset={current.reference.dataset} /></>}
    {preserved&&<><NutrientTiles values={original!.nutrients} unit={unit} /><p className="tiny-note">Unchanged reference and grams retain the saved nutrition snapshot.</p>{original!.reference&&<SourceNote dataset={original!.reference.dataset} />}</>}
  </section>
}
