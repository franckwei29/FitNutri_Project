import {test} from 'node:test'
import assert from 'node:assert/strict'
import {mkdtempSync,writeFileSync,readFileSync,statSync,symlinkSync,rmSync} from 'node:fs'
import {tmpdir} from 'node:os'
import {join} from 'node:path'
import {stageBackendArtifact} from '../../deploy/stage-backend.mjs'
function fixture(run){const root=mkdtempSync(join(tmpdir(),'fitnutri-artifact-'));try{return run(root)}finally{rmSync(root,{recursive:true,force:true})}}
test('repackaging the build path cannot mutate the immutable running copy',()=>fixture(root=>{const source=join(root,'build.jar'),dest=join(root,'runtime');writeFileSync(source,Buffer.from([0x50,0x4b,3,4,1,2,3]));const first=stageBackendArtifact(source,dest),bytes=readFileSync(first);writeFileSync(source,Buffer.from([0x50,0x4b,3,4,9,8,7]));const second=stageBackendArtifact(source,dest);assert.notEqual(first,second);assert.deepEqual(readFileSync(first),bytes);assert.equal(statSync(first).mode&0o777,0o400);assert.equal(statSync(dest).mode&0o777,0o700);assert.equal(stageBackendArtifact(source,dest),second)}))
test('invalid build and symlink destination cannot be staged as a runnable artifact',()=>fixture(root=>{const source=join(root,'build.jar');writeFileSync(source,'not a jar');assert.throws(()=>stageBackendArtifact(source,join(root,'runtime')));writeFileSync(source,Buffer.from([0x50,0x4b,3,4,1]));symlinkSync(root,join(root,'linked'));assert.throws(()=>stageBackendArtifact(source,join(root,'linked')))}))
