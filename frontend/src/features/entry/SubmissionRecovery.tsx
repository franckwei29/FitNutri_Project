import {Alert,Button} from '@mui/material'
export function SubmissionRecovery({submissionId,busy,onCheck,onRetry}:{submissionId:string;busy:boolean;onCheck:()=>void;onRetry:()=>void}){
  return <section className="card form-section"><Alert severity="warning">The save result is unknown. It may already be committed. Details stay frozen until the original submission is resolved.</Alert><p>Submission reference: {submissionId}</p><div className="actions"><Button disabled={busy} onClick={onCheck}>Check original receipt</Button><Button disabled={busy} onClick={onRetry}>Retry the same submission</Button></div><p className="tiny-note">A missing receipt does not prove rollback. Retrying keeps the exact original ID and payload.</p></section>
}
