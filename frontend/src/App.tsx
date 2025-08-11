
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline, GlobalStyles } from '@mui/material';
import { SimplePoliceReportForm } from './components/SimplePoliceReportForm';

// Create a police-themed Material-UI theme
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2', // Police blue
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#f57c00', // Amber for warnings/alerts
      light: '#ffb74d',
      dark: '#f57c00',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 600,
      letterSpacing: '-0.5px',
    },
    h6: {
      fontWeight: 500,
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 8,
          fontWeight: 500,
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 8,
          },
        },
      },
    },
  },
});

const globalStyles = (
  <GlobalStyles
    styles={{
      '*': {
        boxSizing: 'border-box',
      },
      html: {
        height: '100%',
      },
      body: {
        height: '100%',
        margin: 0,
        fontFamily: theme.typography.fontFamily,
        backgroundColor: theme.palette.background.default,
      },
      '#root': {
        minHeight: '100vh',
      },
    }}
  />
);

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {globalStyles}
      <SimplePoliceReportForm />
    </ThemeProvider>
  );
}

export default App
