#!/usr/bin/env node
import {existsSync,readFileSync,readdirSync,realpathSync,statSync} from 'node:fs'
import {dirname,extname,isAbsolute,join,relative,resolve,sep} from 'node:path'
import {fileURLToPath} from 'node:url'
import ts from '../frontend/node_modules/typescript/lib/typescript.js'

const forbiddenModule=/(?:^|[/\\])(?:stores?|mock[^/\\]*|seed[^/\\]*|demotools[^/\\]*|fixtures?|__tests__|legacy)(?:[/\\.]|$)/i
const privateField=/\b(?:password_hash|passwordHash|hashedPassword)\b/
const demoAccount=/\b(?:jessica|alex|marcus|priya|admin)@example\.com\b/i
const providerCredential=/\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\b/
const bcrypt=/\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}/
// A literal used only in `field in value` rejects leaked material; it is not a stored hash.
function privateClientField(node){
  if(!(ts.isIdentifier(node)||ts.isStringLiteralLike(node))||!privateField.test(node.text))return false
  const parent=node.parent
  return !(ts.isStringLiteralLike(node)&&parent&&ts.isBinaryExpression(parent)&&parent.left===node&&parent.operatorToken.kind===ts.SyntaxKind.InKeyword)
}

/** Reports file/category only: never emit the credential or private literal itself. */
export function checkProductionBoundaries({root,checkBuild=true}){
  root=realpathSync(root)
  const sourceRoot=join(root,'frontend/src'),dist=join(root,'frontend/dist'),entry=join(sourceRoot,'main.tsx')
  const issues=[],seen=new Set(),edges=[],artifacts=[]
  const add=(file,code)=>{const name=relative(root,file);if(!issues.some(i=>i.file===name&&i.code===code))issues.push({file:name,code})}
  const inside=(base,file)=>file===base||file.startsWith(base+sep)
  function resolveModule(from,specifier){
    if(forbiddenModule.test(specifier)||/^(?:msw|vitest|@mswjs(?:\/|$)|@testing-library\/|@playwright\/test|node:test)/.test(specifier))add(from,'FORBIDDEN_PRODUCTION_MODULE')
    if(/^(?:https?:|data:|file:)/.test(specifier)){add(from,'REMOTE_MODULE');return null}
    if(!specifier.startsWith('.')){if(specifier.startsWith('@/'))add(from,'UNRESOLVED_IMPORT');return null}
    const base=resolve(dirname(from),specifier)
    if(!inside(sourceRoot,base)){add(from,'SOURCE_OUTSIDE_PRODUCTION');return null}
    if(/\.(?:css|svg|png|jpe?g|webp|woff2?)$/i.test(base))return null
    const found=[base,...['.ts','.tsx','.js','.jsx','.mjs'].map(e=>base+e),...['index.ts','index.tsx','index.js'].map(n=>join(base,n))].find(f=>existsSync(f)&&statSync(f).isFile())
    if(!found){add(from,'UNRESOLVED_IMPORT');return null}
    const actual=realpathSync(found)
    if(!inside(sourceRoot,actual)){add(from,'SOURCE_OUTSIDE_PRODUCTION');return null}
    return actual
  }
  function walk(file){
    if(seen.has(file))return
    seen.add(file)
    if(forbiddenModule.test(relative(sourceRoot,file)))add(file,'FORBIDDEN_PRODUCTION_MODULE')
    const text=readFileSync(file,'utf8')
    if(providerCredential.test(text))add(file,'PROVIDER_CREDENTIAL')
    if(bcrypt.test(text))add(file,'PRIVATE_CLIENT_FIELD')
    const ast=ts.createSourceFile(file,text,ts.ScriptTarget.Latest,true,file.endsWith('.tsx')?ts.ScriptKind.TSX:ts.ScriptKind.TS)
    function dependency(specifier,kind){const target=resolveModule(file,specifier);if(target){edges.push({from:relative(root,file),to:relative(root,target),kind});walk(target)}}
    function visit(node){
      if(privateClientField(node))add(file,'PRIVATE_CLIENT_FIELD')
      if((ts.isIdentifier(node)||ts.isStringLiteralLike(node))&&/^(?:localStorage|sessionStorage)$/.test(node.text))add(file,'BROWSER_STORAGE')
      if(ts.isStringLiteralLike(node)&&demoAccount.test(node.text))add(file,'DEMO_ACCOUNT')
      if((ts.isImportDeclaration(node)||ts.isExportDeclaration(node))&&node.moduleSpecifier&&ts.isStringLiteralLike(node.moduleSpecifier))dependency(node.moduleSpecifier.text,'static')
      if(ts.isCallExpression(node)&&(node.expression.kind===ts.SyntaxKind.ImportKeyword||(ts.isIdentifier(node.expression)&&node.expression.text==='require'))){const value=node.arguments[0];if(value&&ts.isStringLiteralLike(value))dependency(value.text,'dynamic');else add(file,'UNTRACEABLE_IMPORT')}
      ts.forEachChild(node,visit)
    }
    visit(ast)
  }
  if(!existsSync(entry))add(entry,'ENTRY_REQUIRED');else walk(realpathSync(entry))
  let manifestEntries=0
  if(checkBuild){
    const manifestFile=join(dist,'.vite/manifest.json')
    if(!existsSync(manifestFile)||!existsSync(join(dist,'index.html')))add(dist,'BUILD_REQUIRED')
    else{
      try{
        const manifest=JSON.parse(readFileSync(manifestFile,'utf8'));manifestEntries=Object.keys(manifest).length
        if(!Object.values(manifest).some(v=>v.isEntry))add(manifestFile,'ENTRY_REQUIRED')
        for(const value of Object.values(manifest)){
          for(const key of [...(value.imports??[]),...(value.dynamicImports??[])])if(!manifest[key])add(manifestFile,'UNRESOLVED_BUILD_IMPORT')
          for(const name of [value.file,...(value.css??[]),...(value.assets??[])]){if(typeof name!=='string'||isAbsolute(name)||!inside(dist,resolve(dist,name))||!existsSync(resolve(dist,name)))add(manifestFile,'INVALID_BUILD_ASSET')}
        }
      }catch{add(manifestFile,'INVALID_BUILD_MANIFEST')}
      function inspect(folder){for(const item of readdirSync(folder,{withFileTypes:true})){const file=join(folder,item.name);if(item.isDirectory())inspect(file);else if(/\.(?:js|mjs|cjs|json|html)$/.test(extname(file))){artifacts.push(relative(root,file));if(forbiddenModule.test(relative(dist,file)))add(file,'FORBIDDEN_PRODUCTION_MODULE');const text=readFileSync(file,'utf8');if(providerCredential.test(text))add(file,'PROVIDER_CREDENTIAL');if(bcrypt.test(text))add(file,'PRIVATE_CLIENT_FIELD');if(demoAccount.test(text))add(file,'DEMO_ACCOUNT');const ast=ts.createSourceFile(file,text,ts.ScriptTarget.Latest,true,ts.ScriptKind.JS);function tokens(node){if(privateClientField(node))add(file,'PRIVATE_CLIENT_FIELD');ts.forEachChild(node,tokens)}tokens(ast)}}}
      inspect(dist)
    }
  }
  return {ok:issues.length===0,sourceFiles:[...seen].map(f=>relative(root,f)).sort(),edges,manifestEntries,artifacts:artifacts.sort(),issues}
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const requested=process.argv.indexOf('--root'),root=requested>=0?process.argv[requested+1]:resolve(dirname(fileURLToPath(import.meta.url)),'..')
  try{const report=checkProductionBoundaries({root});if(process.argv.includes('--json'))console.log(JSON.stringify(report,null,2));else{console.log(`${report.ok?'PASS':'FAIL'}: ${report.sourceFiles.length} reachable source modules, ${report.edges.length} import edges, ${report.manifestEntries} manifest entries, ${report.artifacts.length} production text artifacts; ${report.issues.length} boundary violations.`);for(const issue of report.issues)console.error(`${issue.code}: ${issue.file}`)}process.exitCode=report.ok?0:1}catch{console.error('FAIL: production boundary inspection could not complete. Build the actual frontend and inspect the configured root.');process.exitCode=1}
}
