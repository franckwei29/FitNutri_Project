import type {SubmissionReceiptV2} from './contracts'
import type {EntrySubmissionV2} from './submissionsV2'
import {SubmissionReceiptV2Schema} from './schemas'
import {decodeResponse,UnexpectedApiResponse} from './validators'
type ExpectedRoot={kind:'SINGLE'|'DISH'|'QUICK'|'WORKOUT';uuid:string;leaves:string[]}
/** A schema-valid receipt must still prove every exact requested identity and parent relationship. */
export function verifyReceiptV2(envelope:EntrySubmissionV2,value:unknown):SubmissionReceiptV2 {
 const proof=decodeResponse(SubmissionReceiptV2Schema,value),request=envelope.payload
 const roots:ExpectedRoot[]=envelope.domain==='FOOD'?envelope.payload.entries.map(e=>({kind:e.kind,uuid:e.clientEntryId,leaves:e.kind==='SINGLE'?[e.clientEntryId]:e.ingredients.map(i=>i.clientEntryId)})):[{kind:envelope.payload.entry.kind,uuid:envelope.payload.entry.clientEntryId,leaves:envelope.payload.entry.kind==='QUICK'?[envelope.payload.entry.period.clientEntryId]:envelope.payload.entry.periods.map(p=>p.clientEntryId)}]
 const expectedOperation=envelope.domain==='FOOD'?'FOOD_BATCH_V2':'EXERCISE_ENTRY_V2',expectedLeaf=envelope.domain==='FOOD'?'FOOD_COMPONENT':'ACTIVITY_PERIOD'
 const fail=()=>{throw new UnexpectedApiResponse()}
 if(proof.submissionId!==request.submissionId||proof.kind!==expectedOperation||proof.roots.length!==roots.length||proof.leaves.length!==roots.reduce((n,r)=>n+r.leaves.length,0))fail()
 // Input identities cannot overlap, except each SINGLE/QUICK root and its own leaf.
 const logicalInputs=roots.flatMap(r=>r.kind==='SINGLE'||r.kind==='QUICK'?[r.uuid]:[r.uuid,...r.leaves])
 if(new Set(logicalInputs).size!==logicalInputs.length)fail()
 if(new Set(proof.roots.map(r=>`${r.kind}:${r.id}`)).size!==proof.roots.length||new Set(proof.leaves.map(l=>`${l.kind}:${l.id}`)).size!==proof.leaves.length||new Set(proof.leaves.map(l=>l.clientEntryId)).size!==proof.leaves.length)fail()
 if(new Set(proof.roots.map(r=>r.clientEntryId)).size!==proof.roots.length)fail()
 for(const root of roots){
  const saved=proof.roots.find(r=>r.clientEntryId===root.uuid)
  if(!saved||saved.kind!==root.kind||saved.recordDate!==request.recordDate||saved.versionAtCreation!==0)fail()
  for(const uuid of root.leaves){
   const leaf=proof.leaves.find(l=>l.clientEntryId===uuid)
   if(!leaf||leaf.kind!==expectedLeaf||leaf.rootClientEntryId!==root.uuid||leaf.recordDate!==request.recordDate||leaf.versionAtCreation!==0)fail()
   if((root.kind==='SINGLE'||root.kind==='QUICK')&&(uuid!==root.uuid||leaf?.id!==saved?.id))fail()
  }
 }
 return proof
}
