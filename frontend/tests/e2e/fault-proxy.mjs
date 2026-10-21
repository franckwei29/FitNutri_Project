import {createServer,request as upstreamRequest} from 'node:http'
import {once} from 'node:events'
import {createHash} from 'node:crypto'
import {resolve} from 'node:path'
import {fileURLToPath} from 'node:url'
const paths=new Set(['/api/records/food/batch','/api/records/exercise','/api/v2/food/batches','/api/v2/exercise/entries'])
const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
/** Test-only byte forwarding/drop. No business response fabrication or storage of auth/health bodies. */
export async function createFaultProxy({target='http://127.0.0.1:8080',port=8082}={}){
  const upstream=new URL(target)
  if(upstream.protocol!=='http:'||!['127.0.0.1','localhost'].includes(upstream.hostname)||upstream.username||upstream.password||upstream.pathname!=='/'||upstream.search||upstream.hash)throw Error('The fault proxy requires an explicit credential-free loopback HTTP upstream.')
  let nextDrop=null;const attempts=[]
  const server=createServer(async(incoming,outgoing)=>{
    if(!incoming.url?.startsWith('/')||incoming.url.startsWith('//')){outgoing.writeHead(400);outgoing.end();return}
    const path=new URL(incoming.url,'http://127.0.0.1').pathname
    if(path==='/__test__/status'&&incoming.method==='GET'){outgoing.writeHead(200,{'content-type':'application/json','cache-control':'no-store'});outgoing.end(JSON.stringify({nextDrop,attempts}));return}
    const chunks=[];let size=0
    try{for await(const chunk of incoming){size+=chunk.length;if(size>16*1024*1024)throw Error('Oversized test request');chunks.push(chunk)}}catch{outgoing.writeHead(413);outgoing.end();return}
    const body=Buffer.concat(chunks)
    if(path==='/__test__/control'){
      let control;try{control=JSON.parse(body.toString('utf8'))}catch{control=null}
      if(incoming.method!=='POST'||control?.action!=='DROP_NEXT_ACK'||!paths.has(control.path)){outgoing.writeHead(400,{'content-type':'application/json'});outgoing.end('{"error":"Unsupported test fault control"}');return}
      nextDrop=control.path;outgoing.writeHead(200,{'content-type':'application/json','cache-control':'no-store'});outgoing.end('{"armed":true}');return
    }
    let attempt=null,drop=false
    if(incoming.method==='POST'&&paths.has(path)){
      let input;try{input=JSON.parse(body.toString('utf8'))}catch{input={}}
      const candidates=Array.isArray(input.items)?input.items.map(x=>x?.clientEntryId):Array.isArray(input.entries)?input.entries.flatMap(root=>[root?.clientEntryId,...(Array.isArray(root?.ingredients)?root.ingredients.map(leaf=>leaf?.clientEntryId):[])]):input.entry?[input.entry.clientEntryId,...(Array.isArray(input.entry.periods)?input.entry.periods.map(period=>period?.clientEntryId):[input.entry.period?.clientEntryId])]:[input.clientEntryId]
      const ids=[...new Set(candidates.filter(x=>typeof x==='string'&&uuid.test(x)))].slice(0,200)
      drop=nextDrop===path;if(drop)nextDrop=null
      attempt={method:'POST',path,bodySha256:createHash('sha256').update(body).digest('hex'),submissionId:typeof input.submissionId==='string'&&uuid.test(input.submissionId)?input.submissionId:null,clientEntryIds:ids,status:null,ackRecordIds:[],ackRootKeys:[],dropped:false,clientGone:false}
      attempts.push(attempt);if(attempts.length>100)attempts.shift()
    }
    outgoing.on('close',()=>{if(attempt&&attempt.status===null)attempt.clientGone=true})
    const headers={...incoming.headers,host:upstream.host,'content-length':String(body.length)}
    delete headers.connection;delete headers['transfer-encoding'];delete headers['accept-encoding']
    const request=upstreamRequest(new URL(incoming.url,upstream),{method:incoming.method,headers},response=>{
      const data=[];response.on('data',chunk=>data.push(chunk));response.on('end',()=>{
        const bytes=Buffer.concat(data)
        if(attempt){attempt.status=response.statusCode;try{const proof=JSON.parse(bytes.toString('utf8'));const records=proof?.schemaVersion===2?proof.leaves:proof?.records;attempt.ackRecordIds=Array.isArray(records)?records.map(x=>x?.id).filter(x=>typeof x==='string'&&/^[1-9][0-9]*$/.test(x)).slice(0,200):[];attempt.ackRootKeys=proof?.schemaVersion===2&&Array.isArray(proof.roots)?proof.roots.filter(root=>['SINGLE','DISH','QUICK','WORKOUT'].includes(root?.kind)&&typeof root?.id==='string'&&/^[1-9][0-9]*$/.test(root.id)).slice(0,20).map(root=>`${root.kind}:${root.id}`):[]}catch{/* No raw error/provider payload is retained. */}attempt.clientGone=attempt.clientGone||outgoing.destroyed}
        if(drop&&response.statusCode>=200&&response.statusCode<300){attempt.dropped=true;outgoing.destroy();return}
        if(outgoing.destroyed)return
        const returned={...response.headers};delete returned.connection;delete returned['transfer-encoding'];returned['content-length']=String(bytes.length)
        outgoing.writeHead(response.statusCode,returned);outgoing.end(bytes)
      })
    })
    request.on('error',()=>{if(attempt)attempt.status=502;if(!outgoing.destroyed){outgoing.writeHead(502,{'content-type':'text/plain'});outgoing.end('The local test upstream could not be reached.')}})
    // Client timeout does not cancel an already dispatched server transaction.
    request.end(body)
  })
  server.listen(port,'127.0.0.1');await once(server,'listening')
  return {origin:`http://127.0.0.1:${server.address().port}`,close:async()=>{server.closeAllConnections();await new Promise(r=>server.close(r))}}
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const port=Number(process.env.FITNUTRI_E2E_FAULT_PORT??8082),target=process.env.FITNUTRI_E2E_UPSTREAM??'http://127.0.0.1:8080';if(!Number.isSafeInteger(port)||port<1||port>65535)throw Error('Choose an explicit local test proxy port.')
  const proxy=await createFaultProxy({target,port});console.log('FitNutri test-only fault proxy ready on '+proxy.origin+'; forwards unchanged real API traffic to the validated loopback upstream.')
  for(const signal of ['SIGINT','SIGTERM'])process.once(signal,()=>void proxy.close().then(()=>process.exit(0)))
}
