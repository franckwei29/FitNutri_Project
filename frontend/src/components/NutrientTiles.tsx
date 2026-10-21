import type {EnergyUnit,Nutrients} from '../api/contracts'
import {displayEnergy} from '../domain/units'
/** Demo tiles, displaying canonical server values without recalculating nutrients. */
export function NutrientTiles({values,unit}:{values:Nutrients;unit:EnergyUnit}){
  const grams=(value:number|null)=>value===null?'Unavailable':`${new Intl.NumberFormat('en-AU',{maximumFractionDigits:1}).format(value)} g`
  return <div className="nutrient-tiles">{[['Energy',displayEnergy(values.energyKj,unit)],['Protein',grams(values.proteinGrams)],['Carbs',grams(values.carbsGrams)],['Fat',grams(values.fatGrams)]].map(([label,value])=><div key={label}><span>{label}</span><strong>{value}</strong></div>)}</div>
}
