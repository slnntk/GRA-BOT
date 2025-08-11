# GRA-BOT Police Report System - Frontend

This React TypeScript frontend provides an intelligent police report generation system with advanced UI/UX features designed for GTA RP scenarios.

## ✨ Key Features Implemented

### 🔧 Fixed Numeric Input Issues
- **Continuous typing support**: Numeric inputs maintain focus during multi-digit entry
- **Enhanced user experience**: No more clicking after each digit
- **Smart focus behavior**: Auto-selects text on first focus, allows incremental editing

### 🔍 Intelligent Autocomplete
- **Crime field**: Fuzzy search through 22+ predefined crimes with descriptions
- **Location fields**: Smart search through 130+ GTA RP locations
- **Performance optimized**: 300ms debounced search, shows results after 2+ characters
- **User-friendly**: Keyboard navigation, tooltips with descriptions

### 👥 Dynamic People Count with Pluralization
- **Interactive slider**: Select 1-10 people involved
- **Smart pluralization**: Automatically adjusts Portuguese grammar
  - "1 pessoa" → "2 pessoas"
  - "o suspeito estava" → "os suspeitos estavam"
  - "foi acionado" → "foram acionados"

### 📄 Real-time Report Preview
- **Live updates**: Report generates as you type
- **Professional formatting**: Complete police report template in Portuguese
- **Smart formatting**: 
  - Time conversion (120 minutes → "2 horas")
  - Currency formatting (15000 → "$15.000")
  - Date/time in Brazilian format

## 🛠 Technology Stack

- **React 18** with TypeScript
- **Vite** for fast development
- **Material-UI (MUI)** for professional design
- **React Hook Form** + **Yup** for form management and validation
- **Fuse.js** for fuzzy search functionality
- **Framer Motion** for smooth animations

## 🚀 Getting Started

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 to view the application.

## 📱 Responsive Design

The form is fully responsive and works on:
- Desktop computers
- Tablets
- Mobile devices

## ♿ Accessibility

- ARIA labels for screen readers
- Keyboard navigation support
- High contrast design
- Focus management

## 🎨 UI/UX Features

- **Police-themed design** with blue color scheme
- **Smooth animations** for better user experience
- **Error handling** with clear validation messages
- **Loading states** and visual feedback
- **Professional layout** with cards and proper spacing

## 📊 Data

### Crimes Database
22+ predefined crimes including:
- Roubo a Mão Armada
- Furto de Veículo
- Homicídio
- Tráfico de Drogas
- And many more...

### Locations Database
130+ GTA RP locations including:
- Los Santos areas (Downtown, Vinewood, etc.)
- Sandy Shores and Blaine County
- Specific landmarks (banks, police stations, etc.)
- Government and commercial buildings

## 🔄 Integration

The frontend is designed to integrate with the existing Java Spring Boot backend. Form data is structured to be easily consumed by REST API endpoints.

## 📝 Example Usage

1. **Select Crime**: Type "roubo" to see robbery options
2. **Choose Locations**: Search for "downtown" or "bank"
3. **Set People Count**: Use slider to select number of suspects
4. **Add Penalties**: Enter fines and prison time
5. **Add Observations**: Include additional case details
6. **Generate Report**: View live preview and submit

The system automatically handles Portuguese pluralization and generates professional police reports suitable for GTA RP scenarios.