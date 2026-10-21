import type {Fact} from '../api/contracts'
/** Server facts are displayed without a second rules engine, model call or consent gate. */
export function RuleSummary({facts}:{facts:Fact[]}){
  return <section aria-label="Recorded facts"><h3>Facts from confirmed records</h3>{facts.length?<ul className="recorded-facts">{facts.map((fact,index)=><li key={`${fact.code}/${index}`}>{fact.text}</li>)}</ul>:<p>No summary facts were returned for this view.</p>}<p className="tiny-note">These describe recorded data and its coverage. They do not fill unrecorded meals or activity.</p></section>
}
