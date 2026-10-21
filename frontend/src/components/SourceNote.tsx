import type {Dataset} from '../api/contracts'
export function SourceNote({dataset}:{dataset:Dataset}){
  const url=dataset.sourceUrl?.startsWith('https://')?dataset.sourceUrl:null
  return <p className="source-note">{dataset.name} · {dataset.release} · {dataset.revision}{dataset.verified?' · Verified source':' · Unverified legacy source'}{url&&<> · <a href={url} target="_blank" rel="noreferrer">Official dataset source</a></>}</p>
}
