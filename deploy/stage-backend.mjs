/** Keep running JVM artifacts independent of Maven's mutable target path. */
import {readFileSync,mkdirSync,lstatSync,openSync,writeFileSync,fsyncSync,closeSync,chmodSync} from 'node:fs'
import {createHash} from 'node:crypto'
import {resolve} from 'node:path'
import {fileURLToPath} from 'node:url'
const digest=bytes=>createHash('sha256').update(bytes).digest('hex')
export function stageBackendArtifact(source,directory){
 const info=lstatSync(source);if(!info.isFile()||info.size>256*1024*1024)throw Error('A built regular JAR is required.')
 const bytes=readFileSync(source);if(bytes.length<4||bytes.readUInt32LE(0)!==0x04034b50)throw Error('A built JAR archive is required.')
 mkdirSync(directory,{recursive:true,mode:0o700});if(!lstatSync(directory).isDirectory()||lstatSync(directory).isSymbolicLink())throw Error('A private regular runtime directory is required.')
 const sha=digest(bytes),path=resolve(directory,`backend-${sha}.jar`)
 try{const fd=openSync(path,'wx',0o400);try{writeFileSync(fd,bytes);fsyncSync(fd)}finally{closeSync(fd)}}catch(error){if(error.code!=='EEXIST')throw error}
 if(!lstatSync(path).isFile()||lstatSync(path).isSymbolicLink()||digest(readFileSync(path))!==sha)throw Error('Staged artifact verification failed.')
 chmodSync(path,0o400);return path
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 try{const root=resolve(fileURLToPath(new URL('..',import.meta.url)));console.log(stageBackendArtifact(resolve(root,'backend/target/backend-0.0.1-SNAPSHOT.jar'),resolve(root,'.local/runtime')))}catch{console.error('Cannot stage a verified immutable backend JAR. Run the build first.');process.exitCode=1}
}
