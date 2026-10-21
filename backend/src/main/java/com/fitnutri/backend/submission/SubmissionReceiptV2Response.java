package com.fitnutri.backend.submission;
import java.time.*;
import java.util.*;
/** Minimal identity proof only; record content is always read separately through current owned endpoints. */
public record SubmissionReceiptV2Response(int schemaVersion,UUID submissionId,SubmissionOperation kind,Instant committedAt,boolean replayed,List<Root> roots,List<Leaf> leaves){
 public record Root(String kind,String id,UUID clientEntryId,LocalDate recordDate,int versionAtCreation){}
 public record Leaf(String kind,String id,UUID clientEntryId,UUID rootClientEntryId,LocalDate recordDate,int versionAtCreation){}
 public record Proof(int schemaVersion,List<Root> roots,List<Leaf> leaves){public Proof{roots=List.copyOf(roots);leaves=List.copyOf(leaves);}}
 public static void validateTypedProof(CanonicalEntryHasherV2.Submission input,Proof proof){
  if(proof==null||proof.schemaVersion()!=2||proof.roots().size()!=input.roots().size())throw invalid();
  Map<UUID,CanonicalEntryHasherV2.ExpectedRoot> expected=new HashMap<>();int count=0;for(var root:input.roots()){expected.put(root.clientEntryId(),root);count+=root.leaves().size();}if(proof.leaves().size()!=count)throw invalid();
  var rootUuids=new HashSet<UUID>();var rootIds=new HashSet<String>();var roots=new HashMap<UUID,Root>();
  for(var root:proof.roots()){var wanted=expected.get(root.clientEntryId());if(wanted==null||!wanted.kind().equals(root.kind())||!rootUuids.add(root.clientEntryId())||!validId(root.id())||!rootIds.add(root.kind()+":"+root.id())||!input.recordDate().equals(root.recordDate())||root.versionAtCreation()!=0)throw invalid();roots.put(root.clientEntryId(),root);}
  var leafUuids=new HashSet<UUID>();var leafIds=new HashSet<String>();String leafKind=input.operation()==SubmissionOperation.FOOD_BATCH_V2?"FOOD_COMPONENT":"ACTIVITY_PERIOD";
  for(var leaf:proof.leaves()){var parent=expected.get(leaf.rootClientEntryId());var root=roots.get(leaf.rootClientEntryId());if(parent==null||root==null||!parent.leaves().contains(leaf.clientEntryId())||!leafKind.equals(leaf.kind())||!leafUuids.add(leaf.clientEntryId())||!validId(leaf.id())||!leafIds.add(leaf.kind()+":"+leaf.id())||!input.recordDate().equals(leaf.recordDate())||leaf.versionAtCreation()!=0)throw invalid();if(Set.of("SINGLE","QUICK").contains(root.kind())&&(!root.clientEntryId().equals(leaf.clientEntryId())||!root.id().equals(leaf.id())))throw invalid();}
 }
 private static boolean validId(String value){if(value==null||!value.matches("[1-9][0-9]{0,18}"))return false;try{return Long.parseLong(value)>0;}catch(NumberFormatException e){return false;}}
 private static IllegalStateException invalid(){return new IllegalStateException("Creation did not return the complete expected typed identity proof.");}
}
