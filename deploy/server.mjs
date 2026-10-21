/** Built-in Node static/API gateway. No Vite runtime, provider key or business data store. */
import http from 'node:http'
import https from 'node:https'
import {createReadStream,readFileSync,realpathSync,statSync} from 'node:fs'
import {resolve,sep,extname} from 'node:path'
import {randomUUID} from 'node:crypto'
import {fileURLToPath} from 'node:url'
const routes=new Set(['/','/login','/onboarding','/profile','/add','/entry','/review','/dashboard','/history','/chat','/admin'])
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.jpg':'image/jpeg','.webp':'image/webp','.woff2':'font/woff2'}
const hop=['connection','keep-alive','proxy-authenticate','proxy-authorization','te','trailer','transfer-encoding','upgrade']
function cleanHeaders(headers){const result={...headers};for(const key of String(headers.connection??'').split(','))delete result[key.trim().toLowerCase()];for(const key of hop)delete result[key];return result}
function apiError(res,status,code,path,message){if(res.headersSent){res.destroy();return}res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify({status,code,message,path:path.split('?')[0],requestId:randomUUID(),fieldErrors:[],retryable:status===502||status===504}))}
export function loadDeploymentConfig(env=process.env){
 const host=env.HOST??'127.0.0.1',port=Number(env.PORT??8787),upstream=env.BACKEND_ORIGIN??'http://127.0.0.1:8080',url=new URL(upstream)
 if(!['http:','https:'].includes(url.protocol)||!['127.0.0.1','localhost','[::1]'].includes(url.hostname)||url.username||url.password||url.search||url.hash||url.pathname!=='/')throw Error('Backend must be an explicit loopback HTTP(S) origin.')
 if(!Number.isInteger(port)||port<1||port>65535)throw Error('Invalid deployment port.')
 let tls
 if(env.FITNUTRI_LOCAL_HTTP==='1'){if(!['127.0.0.1','localhost','::1'].includes(host))throw Error('Local HTTP must bind loopback only.')}else{if(!env.TLS_CERT_FILE||!env.TLS_KEY_FILE)throw Error('HTTPS requires TLS_CERT_FILE and TLS_KEY_FILE; local HTTP requires explicit FITNUTRI_LOCAL_HTTP=1.');tls={cert:readFileSync(env.TLS_CERT_FILE),key:readFileSync(env.TLS_KEY_FILE),minVersion:'TLSv1.2'}}
 return{host,port,upstream,tls,distRoot:resolve(fileURLToPath(new URL('../frontend/dist/',import.meta.url))),maxBodyBytes:12*1024*1024,upstreamTimeoutMs:85000}
}
export function createApplicationServer({distRoot,upstream,maxBodyBytes=12*1024*1024,upstreamTimeoutMs=85000,tls}){
 const root=realpathSync(distRoot),target=new URL(upstream)
 // Synchronous preflight: never start a misleading empty application server.
 if(!statSync(resolve(root,'index.html')).isFile())throw Error('Build frontend first.')
 const handler=(req,res)=>{
  res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','same-origin')
  if(!req.url?.startsWith('/')||req.url.startsWith('//')){res.writeHead(400,{'Cache-Control':'no-store'});res.end();return}
  let pathname;try{pathname=decodeURIComponent(new URL(req.url,'http://localhost').pathname)}catch{res.writeHead(400);res.end();return}
  if(pathname==='/api'||pathname.startsWith('/api/')){
   if(Number(req.headers['content-length']??0)>maxBodyBytes){apiError(res,413,'PAYLOAD_TOO_LARGE',pathname,'The uploaded request exceeds the supported size.');req.resume();return}
   const headers=cleanHeaders(req.headers);delete headers.host;for(const key of Object.keys(headers))if(key==='forwarded'||key.startsWith('x-forwarded-'))delete headers[key]
   // Do not trust arbitrary client forwarding headers; TLS/cookie policy remains server-configured.
   const transport=target.protocol==='https:'?https:http
   let bytes=0,finished=false
   const proxy=transport.request(new URL(req.url,target),{method:req.method,headers},reply=>{
    if(finished){reply.resume();return}const output=cleanHeaders(reply.headers);output['cache-control']='no-store';res.writeHead(reply.statusCode??502,output);reply.pipe(res);reply.on('error',()=>res.destroy())
   })
   const timer=setTimeout(()=>{finished=true;apiError(res,504,'PROVIDER_TIMEOUT',pathname,'The application server did not respond in time.');proxy.destroy()},upstreamTimeoutMs);timer.unref()
   const dispose=()=>clearTimeout(timer)
   proxy.on('error',()=>{dispose();if(!finished){finished=true;apiError(res,502,'PROVIDER_BAD_RESPONSE',pathname,'The application server is unavailable. Try again.')}})
   proxy.on('drain',()=>req.resume());res.on('finish',dispose);res.on('close',()=>{dispose();if(!res.writableEnded){finished=true;proxy.destroy()}})
   req.on('data',part=>{bytes+=part.length;if(bytes>maxBodyBytes){if(!finished){finished=true;apiError(res,413,'PAYLOAD_TOO_LARGE',pathname,'The uploaded request exceeds the supported size.');proxy.destroy();dispose()}return}if(!finished&&!proxy.write(part))req.pause()})
   req.on('end',()=>{if(!finished)proxy.end()});req.on('error',()=>{finished=true;proxy.destroy();dispose()});return
  }
  if(!['GET','HEAD'].includes(req.method)){res.writeHead(405,{'Allow':'GET, HEAD','Cache-Control':'no-store'});res.end();return}
  let file,spa=routes.has(pathname)
  if(spa)file=resolve(root,'index.html')
  else if(/^\/assets\/[A-Za-z0-9_.-]+$/.test(pathname)||pathname==='/favicon.svg')file=resolve(root,'.'+pathname)
  else{res.writeHead(404,{'Cache-Control':'no-store'});res.end();return}
  try{file=realpathSync(file);if(!file.startsWith(root+sep)||!statSync(file).isFile())throw Error();const size=statSync(file).size;res.writeHead(200,{'Content-Type':types[extname(file)]??'application/octet-stream','Content-Length':size,'Cache-Control':spa?'no-store':pathname.startsWith('/assets/')?'public, max-age=31536000, immutable':'public, max-age=3600'});if(req.method==='HEAD')res.end();else createReadStream(file).on('error',()=>res.destroy()).pipe(res)}catch{res.writeHead(404,{'Cache-Control':'no-store'});res.end()}
 }
 const server=tls?https.createServer(tls,handler):http.createServer(handler)
 server.requestTimeout=100000;server.headersTimeout=10000;server.keepAliveTimeout=5000
 return server
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 try{const config=loadDeploymentConfig(),server=createApplicationServer(config);server.listen(config.port,config.host,()=>console.log(`FitNutri static/API gateway ready on ${config.tls?'HTTPS':'explicit local HTTP'} loopback/configured interface; no Vite runtime.`));for(const signal of ['SIGTERM','SIGINT'])process.on(signal,()=>{server.close(()=>process.exit(0));setTimeout(()=>process.exit(1),10000).unref()})}catch{console.error('Gateway startup failed. Check build, loopback backend, explicit HTTP or HTTPS certificate configuration.');process.exitCode=1}
}
