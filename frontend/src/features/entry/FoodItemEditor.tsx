import {useEffect} from 'react'
import {Alert,Button,IconButton,LinearProgress,TextField} from '@mui/material'
import {Close} from '@mui/icons-material'
import type {EnergyUnit,FoodPreview,MealType,FoodRecord} from '../../api/contracts'
import type {DraftAction,DraftFood} from './draftTypes'
import type {CatalogueScope} from '../../hooks/useFoodSearch'
import {useFoodPreview} from '../../hooks/useFoodPreview'
import {parseQuantity} from '../../domain/inputRules'
import {mealLabels} from '../../domain/labels'
import {FoodReferencePicker} from './FoodReferencePicker'
import {SelectField} from '../../components/presentation'
import {NutrientTiles} from '../../components/NutrientTiles'
import {foodInputSignature,retainsFoodSnapshot} from './previewIdentity'
export interface RowPreview {signature:string;preview:FoodPreview|null}
export function FoodItemEditor({row,index,scope,dispatch,unit,disabled=false,recordId,original,onPreview}:{row:DraftFood;index:number;scope:CatalogueScope;dispatch:(action:DraftAction)=>unknown;unit:EnergyUnit;disabled?:boolean;recordId?:string;original?:FoodRecord;onPreview:(rowKey:string,result:RowPreview)=>void}){
  const preserved=!!original&&retainsFoodSnapshot(row,original)
  const result=useFoodPreview(preserved?null:row.referenceId,row.quantityGramsInput,scope,recordId),current=result.ready?result.data??null:null
  const signature=foodInputSignature(row,scope),quantity=parseQuantity(row.quantityGramsInput,'grams'),number=index+1
  useEffect(()=>{onPreview(row.rowKey,{signature,preview:current})},[row.rowKey,signature,current,onPreview])
  const owner={ownerId:scope.ownerId,rowKey:row.rowKey}
  return <section className="food-edit-card" aria-label={`Food item ${number}`}>
    <div className="food-edit-heading"><span className="item-number">{String(number).padStart(2,'0')}</span><strong>{row.selectionShortName??row.selectedReference?.displayName??'Choose a food'}</strong>{!recordId&&<IconButton aria-label={`Remove food ${number}`} disabled={disabled} onClick={()=>dispatch({type:'REMOVE_FOOD',...owner})}><Close fontSize="small" /></IconButton>}</div>
    {row.suggestion&&<section aria-label={`${row.suggestion.origin==='IMAGE'?'Image':'Voice'} suggestion for food ${number}`}><p>{row.suggestion.origin==='IMAGE'?'Image':'Voice'} suggestion: {row.suggestion.proposedName}</p>{row.suggestion.warnings.map((text,i)=><p className="tiny-note" key={i}>{text}</p>)}<div className="actions">{row.suggestion.referenceMatches.map((reference,i)=><Button key={reference.referenceId} disabled={disabled} aria-label={`Choose suggested reference ${i+1} for food ${number}`} onClick={()=>dispatch({type:'SELECT_REFERENCE',...owner,reference})}>{reference.displayName}</Button>)}</div><p className="tiny-note">Choose a matching reference or search below. Review the selected food and actual grams before saving.</p></section>}
    <FoodReferencePicker scope={scope} value={row.selectedReference} disabled={disabled} label={`Reference food ${number}`} selectionTarget={row.rowKey} shortName={row.selectionShortName} gramsInput={row.quantityGramsInput} onUseLastGrams={value=>dispatch({type:'CHANGE_GRAMS',...owner,value})} onSelect={(reference,metadata)=>dispatch(reference?{type:'SELECT_REFERENCE',...owner,reference,metadata}:{type:'CLEAR_REFERENCE',...owner})} />
    <div className="form-grid food-fields"><TextField label={`Portion in grams ${number}`} value={row.quantityGramsInput} disabled={disabled} onChange={e=>dispatch({type:'CHANGE_GRAMS',...owner,value:e.target.value})} error={!!row.quantityGramsInput&&!quantity.ok} helperText={row.quantityGramsInput&&!quantity.ok?quantity.message:'Use grams of the selected food.'} slotProps={{htmlInput:{inputMode:'decimal'}}} />
    <SelectField label={`Meal occasion ${number}`} value={row.mealType} disabled={disabled} onChange={value=>dispatch({type:'CHANGE_MEAL',...owner,value:value as MealType|''})} options={mealLabels} empty="Choose a meal" /></div>
    {result.isFetching&&<LinearProgress aria-label={`Loading nutrition for food ${number}`} />}
    {result.isError&&<><Alert severity="error">{result.error instanceof Error?result.error.message:'Nutrition preview unavailable.'}</Alert><Button disabled={disabled||!quantity.ok||!row.referenceId} onClick={()=>void result.refetch()}>Retry nutrition for food {number}</Button></>}
    {current&&<NutrientTiles values={current.nutrients} unit={unit} />}
    {preserved&&<><NutrientTiles values={original!.nutrients} unit={unit} /><p className="tiny-note">Unchanged reference and grams retain the saved nutrition snapshot.</p></>}

  </section>
}
