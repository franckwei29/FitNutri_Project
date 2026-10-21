// Reused from FitNutri Demo src/main.tsx; provenance in docs/implementation/source-map.md.
import { createTheme } from "@mui/material";
export const theme = createTheme({
  palette: {
    primary: { main: "#6d8f59", dark: "#4c703d" },
    secondary: { main: "#7c9fb8" },
    background: { default: "#f6f8f5", paper: "#fff" },
    text: { primary: "#202c3b", secondary: "#6b7e93" },
    error: { main: "#bd4343" },
  },
  typography: {
    fontFamily: '"Avenir Next", "Trebuchet MS", Arial, sans-serif',
    button: { textTransform: "none", fontWeight: 600 },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 10, minHeight: 44, paddingInline: 20 },
      },
    },
    MuiTextField: { defaultProps: { size: "small", fullWidth: true } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: { background: "#fff", minHeight: 46 },
        notchedOutline: { borderColor: "#dce4e9" },
      },
    },
    MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
    MuiDialog: { defaultProps: { fullWidth: true, maxWidth: "sm" } },
  },
});
