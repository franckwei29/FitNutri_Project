// Auth layout, visual structure and interactions reused from Demo account.tsx.
import { useRef,useState } from 'react'
import { Button,TextField,InputAdornment,IconButton } from '@mui/material'
import { ArrowForward,CheckCircleOutlined,Visibility,VisibilityOff,SpaOutlined,LockOutlined } from '@mui/icons-material'
import { Brand,ErrorBox,LoadingButton } from '../components/presentation'
import { focusError,messageOf } from '../components/formHelpers'
import { useSession } from '../hooks/useSession'
import { register } from '../api/auth'
import {TransientSuccessAlert,type SuccessNotice} from '../components/TransientSuccessAlert'
export function AuthPage() {
  const { actions }=useSession();
  const lock=useRef(false);
  const [notice,setNotice]=useState<SuccessNotice|null>(null);
  const noticeSequence=useRef(0);
  const [mode, setMode] = useState<"login" | "register">("login"),
    [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [name, setName] = useState(""),
    [confirmation, setConfirmation] = useState(""),
    [visible, setVisible] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [errors, setErrors] = useState<Record<string,string>>({});
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (lock.current) return;
    const errs: Record<string,string> = {};
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      errs.email = "Enter a valid email address.";
    if (!password) errs.password = "Enter your password.";
    if (mode === "register") {
      if (!name.trim()) errs.name = "Enter your display name.";
      if (password.length < 8 || !/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) errs.password = "Use at least 8 characters, including a letter and a number.";
      if (new TextEncoder().encode(password).length>72) errs.password="Use at most 72 UTF-8 bytes.";
      if (name.trim().length>100) errs.name="Use at most 100 characters.";
      if (confirmation !== password)
        errs.confirmation = "Passwords do not match.";
    }
    setErrors(errs);
    if (Object.keys(errs).length) {
      focusError();
      return;
    }
    lock.current=true;
    setBusy(true);
    setError("");
    setNotice(null);
    try {
      if (mode === "register") {
        await register({displayName:name,email,password});
        setNotice({id:++noticeSequence.current,message:'Account created. Sign in to set up your profile.'});
        setPassword("");
        setMode("login");
        setConfirmation("");
      } else {
        await actions.signIn({email,password});
      }
    } catch (e) {
      setError(messageOf(e));
    } finally {
      lock.current=false;
      setBusy(false);
    }
  }
  return (
    <main id="main-content" tabIndex={-1} className="auth-layout">
      <section className="auth-brand">
        <Brand />
        <div className="auth-story">
          <p className="eyebrow">FOR AUSTRALIAN ADULTS</p>
          <h1>
            Track nutrition.
            <br />
            Move with purpose.
          </h1>
          <p>
            A clearer picture of your everyday food and movement. At your own
            pace.
          </p>
          <div className="auth-leaves">
            <SpaOutlined />
          </div>
          <div className="auth-points">
            <span>
              <CheckCircleOutlined /> Simple daily records
            </span>
            <span>
              <CheckCircleOutlined /> Thoughtful, transparent estimates
            </span>
            <span>
              <CheckCircleOutlined /> Your information, in perspective
            </span>
          </div>
        </div>
        <p className="auth-foot">
          FitNutri Assistant <span>Your daily wellbeing</span>
        </p>
      </section>
      <section className="auth-form-area">
        <div className="auth-form">
          <div className="auth-top">
            <span className="tag">FITNUTRI</span>
            <span>For your everyday wellbeing</span>
          </div>
          <h1>
            {mode === "login" ? "Welcome back." : "Make room for a new habit."}
          </h1>
          <p className="section-subtitle">
            {mode === "login"
              ? "Sign in to your daily picture."
              : "Create an account to start recording your day."}
          </p>
          <ErrorBox message={error} />
          <TransientSuccessAlert notice={notice} />
          <form onSubmit={submit} noValidate className="stack">
            {mode === "register" && (
              <TextField
                label="Display name"
                autoComplete="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                error={!!errors.name}
                helperText={errors.name}
              />
            )}
            <TextField
              label="Email address"
              type="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={!!errors.email}
              helperText={errors.email}
            />
            <TextField
              label="Password"
              type={visible ? "text" : "password"}
              autoComplete={
                mode === "login" ? "current-password" : "new-password"
              }
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={!!errors.password}
              helperText={
                errors.password ||
                (mode === "register" ? "At least 8 characters, a letter and a number" : undefined)
              }
              slotProps={{
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setVisible(!visible)}
                        aria-label={visible ? "Hide password" : "Show password"}
                      >
                        {visible ? (
                          <VisibilityOff fontSize="small" />
                        ) : (
                          <Visibility fontSize="small" />
                        )}
                      </IconButton>
                    </InputAdornment>
                  ),
                },
              }}
            />
            {mode === "register" && (
              <TextField
                label="Confirm password"
                type={visible ? "text" : "password"}
                autoComplete="new-password"
                value={confirmation}
                onChange={(e) => setConfirmation(e.target.value)}
                error={!!errors.confirmation}
                helperText={errors.confirmation}
              />
            )}
            <LoadingButton
              busy={busy}
              variant="contained"
              type="submit"
              fullWidth
              endIcon={<ArrowForward />}
            >
              {mode === "login" ? "Sign in" : "Create account"}
            </LoadingButton>
          </form>
          <div className="auth-switch">
            {mode === "login" ? "New to FitNutri?" : "Already have an account?"}{" "}
            <Button disabled={busy}
              onClick={() => {
                setMode(mode === "login" ? "register" : "login");
                setError("");
                setErrors({});
              }}
            >
              {mode === "login" ? "Create an account" : "Sign in"}
            </Button>
          </div>
          <p className="auth-disclaimer">
            <LockOutlined fontSize="small" />
            Your account uses a secure server session. Log out when using a shared device.
          </p>
        </div>
      </section>
    </main>
  );
}
