import {mergeConfig} from 'vite'
import actual from '../../vite.config.ts'
/** Same real frontend/URL/origin; only transport passes through the owned one-shot test proxy. */
const target=new URL(process.env.FITNUTRI_E2E_FAULT_ORIGIN??'http://127.0.0.1:8082')
if(target.protocol!=='http:'||!['127.0.0.1','localhost'].includes(target.hostname)||target.username||target.password||target.pathname!=='/'||target.search||target.hash)throw Error('Use an explicit credential-free loopback test fault origin.')
export default mergeConfig(actual,{server:{proxy:{'/api':{target:target.origin,changeOrigin:true}}}})
