interface RootIdentity {id:string;version:number;input:{kind:string}}
/** Offset pages may overlap after another write. Preserve typed identities/order and the newest saved version. */
export function mergeRootPages<T extends RootIdentity>(pages:readonly {results:readonly T[]}[]):T[]{
 const roots=new Map<string,T>()
 for(const page of pages)for(const root of page.results){const key=`${root.input.kind}:${root.id}`,prior=roots.get(key);if(!prior||root.version>prior.version)roots.set(key,root)}
 return [...roots.values()]
}
