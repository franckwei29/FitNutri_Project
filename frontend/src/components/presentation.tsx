// Presentation reused from FitNutri Demo ui.tsx; no store or Database dependency.
import { useId,useRef } from "react";
import type { ReactNode } from "react";
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  CircularProgress,
  MenuItem,
  TextField,
} from "@mui/material";
import { ArrowBack, SpaOutlined } from "@mui/icons-material";
import { Link } from "react-router-dom";
export function Brand() {
  return (
    <Link className="brand" to="/" aria-label="FitNutri Assistant">
      <span className="brand-icon">
        <SpaOutlined />
      </span>
      <span>
        FitNutri<span className="brand-accent"> Assistant</span>
      </span>
    </Link>
  );
}
export function Page({
  title,
  eyebrow,
  subtitle,
  actions,
  children,
  narrow = false,
  back,
}: {
  title: string;
  eyebrow?: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
  narrow?: boolean;
  back?: string;
}) {
  return (
    <main id="main-content" tabIndex={-1} className={`page ${narrow ? "narrow-page" : ""}`}>
      {back && (
        <Button
          component={Link}
          to={back}
          startIcon={<ArrowBack />}
          className="back-link"
        >
          Back
        </Button>
      )}
      <div className="page-heading">
        <div>
          {eyebrow && <p className="eyebrow">{eyebrow}</p>}
          <h1>{title}</h1>
          {subtitle && <p>{subtitle}</p>}
        </div>
        {actions}
      </div>
      {children}
    </main>
  );
}
export function ErrorBox({ message }: { message: string }) {
  return message ? (
    <Alert severity="error" sx={{ mb: 2 }} role="alert">
      {message}
    </Alert>
  ) : null;
}
export function LoadingButton({
  busy,
  children,
  ...props
}: React.ComponentProps<typeof Button> & { busy: boolean }) {
  return (
    <Button
      {...props}
      aria-busy={busy}
      disabled={busy || props.disabled}
      startIcon={
        busy ? <CircularProgress size={17} color="inherit" /> : props.startIcon
      }
    >
      {busy ? "Please wait…" : children}
    </Button>
  );
}
export function ConfirmDialog({
  open,
  title,
  children,
  onCancel,
  cancelLabel = "Cancel",
  onConfirm,
  busy = false,
  label = "Confirm",
  danger = false,
  error = "",
}: {
  open: boolean;
  title: string;
  children: ReactNode;
  onCancel: () => void;
  cancelLabel?: string;
  onConfirm: () => void;
  busy?: boolean;
  label?: string;
  danger?: boolean;
  error?: string;
}) {
  const titleId=useId(),descriptionId=useId(),cancelButton=useRef<HTMLButtonElement>(null);
  return (
    <Dialog
      open={open}
      onClose={busy ? undefined : onCancel}
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      slotProps={{transition:{onEntered:()=>{if(!busy)cancelButton.current?.focus()}}}}
    >
      <DialogTitle id={titleId}>{title}</DialogTitle>
      <DialogContent id={descriptionId} aria-busy={busy}>
        <ErrorBox message={error} />
        {children}
      </DialogContent>
      <DialogActions sx={{ p: 3, pt: 1 }}>
        <Button ref={cancelButton} autoFocus disabled={busy} onClick={onCancel}>
          {cancelLabel}
        </Button>
        <LoadingButton
          busy={busy}
          variant="contained"
          color={danger ? "error" : "primary"}
          onClick={onConfirm}
        >
          {label}
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
}
export function DateField({
  today,
  value,
  onChange,
  error,
  label = "Record date",
}: {
  today: string;
  value: string;
  onChange: (v: string) => void;
  error?: string;
  label?: string;
}) {
  return (
    <TextField
      label={label}
      type="date"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      error={!!error}
      helperText={error}
      slotProps={{
        inputLabel: { shrink: true },
        htmlInput: { max: today, min: "1900-01-01" },
      }}
    />
  );
}
export function SelectField({
  label,
  value,
  onChange,
  options,
  error,
  empty = "Select an option",
  disabled = false,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: Record<string, string>;
  error?: string;
  empty?: string;
  disabled?: boolean;
}) {
  return (
    <TextField
      select
      label={label}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      error={!!error}
      helperText={error}
      disabled={disabled}
    >
      <MenuItem value="">
        <em>{empty}</em>
      </MenuItem>
      {Object.entries(options).map(([v, t]) => (
        <MenuItem key={v} value={v}>
          {t}
        </MenuItem>
      ))}
    </TextField>
  );
}
