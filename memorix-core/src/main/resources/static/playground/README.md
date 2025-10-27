# 🧠 Memorix Playground - New Version

Modern, professional web interface for exploring and managing the Memorix AI Memory Management Framework.

## 🎯 Features

### Core Functionality
- **Dashboard** - Overview with statistics and recent memories
- **Memory Explorer** - Browse, filter, sort, and paginate memories
- **Operations** - Create and update memories with full CRUD support
- **Semantic Search** - AI-powered memory search with advanced options
- **Lifecycle Management** - Decay application and memory cleanup
- **Database Browser** - Multi-datasource configuration and stats

### UI/UX Improvements
- ✅ Professional, clean design (inspired by GitHub, Linear, Notion)
- ✅ Full SPA routing without page reloads
- ✅ Toast notifications for user feedback
- ✅ Modal dialogs for details, editing, confirmations
- ✅ Responsive layout (mobile-friendly)
- ✅ Loading states and skeleton screens
- ✅ Enhanced pagination with per-page selector
- ✅ Filter, sort, and search capabilities

### Technical Features
- ✅ Modular ES6 architecture
- ✅ Centralized API client
- ✅ State management system
- ✅ Reusable component library
- ✅ Proper error handling
- ✅ Professional CSS with design tokens

## 📁 Project Structure

```
playground/
├── index.html              # Main entry point
├── styles/
│   ├── main.css           # Core styles, design system
│   └── components.css     # Component-specific styles
├── js/
│   ├── api.js             # API client
│   ├── state.js           # State management
│   ├── app.js             # Router and initialization
│   ├── components/
│   │   ├── toast.js       # Toast notifications
│   │   ├── modal.js       # Modal dialogs
│   │   ├── memory-card.js # Memory card component
│   │   └── pagination.js  # Pagination component
│   └── pages/
│       ├── dashboard.js   # Dashboard page
│       ├── memories.js    # Memory explorer
│       ├── operations.js  # CRUD operations
│       ├── search.js      # Semantic search
│       ├── lifecycle.js   # Decay management
│       └── databases.js   # Database browser
└── old/                   # Legacy files (backup)
```

## 🚀 Usage

1. Start your Memorix server:
   ```bash
   mvn start -local
   ```

2. Open in browser:
   ```
   http://localhost:8080/playground/index.html
   ```

3. Navigate through pages using the top navigation bar

## 🎨 Design System

### Colors
- **Primary**: Indigo/Blue for actions and highlights
- **Success**: Green for positive feedback
- **Warning**: Orange for cautions
- **Danger**: Red for destructive actions
- **Info**: Blue for informational messages

### Typography
- **Font**: System fonts (Segoe UI, Inter, IBM Plex Sans)
- **Monospace**: SF Mono, Consolas, Monaco

### Components
- Cards with hover effects
- Buttons with multiple variants
- Form inputs with focus states
- Badges for status indicators
- Tables with hover rows
- Modals with backdrop blur
- Toast notifications
- Loading spinners and skeletons

## 🔧 API Integration

All API calls go through the centralized `API` object in `api.js`:

- `API.getMemories(params)` - Get memories with filters
- `API.createMemory(data)` - Create new memory
- `API.updateMemory(id, data)` - Update existing memory
- `API.deleteMemories(userId)` - Delete all memories for user
- `API.searchMemories(request)` - Semantic search
- `API.applyDecay(request)` - Apply lifecycle decay
- `API.getStats(userId)` - Get statistics
- `API.getPlugins()` - Get plugin types
- `API.getPluginConfig(type)` - Get plugin configuration
- `API.getDatasources()` - Get datasource info
- `API.getDatasourceStats()` - Get datasource statistics

## 📝 Notes

### What's New vs Old Version
1. **Modular Architecture** - Split into multiple files instead of one monolithic script
2. **SPA Routing** - No page reloads, smooth navigation
3. **Memory Explorer** - Central view for browsing all memories with filters
4. **Update Support** - Full implementation of UPDATE endpoint
5. **Professional Design** - Modern, clean UI instead of flashy neon
6. **Better UX** - Toast notifications, modals, loading states
7. **State Management** - Proper state handling with reactivity
8. **Reusable Components** - Memory cards, pagination, modals, toasts
9. **Enhanced Search** - Better display of search results and metadata
10. **Database Browser** - Proper multi-datasource visualization

### Future Enhancements (Optional)
- [ ] Bulk operations (select multiple memories)
- [ ] Charts and visualizations for stats
- [ ] Export/Import functionality
- [ ] Dark/Light theme toggle
- [ ] Keyboard shortcuts
- [ ] Real-time updates via WebSocket
- [ ] Advanced filtering with date ranges
- [ ] Memory comparison view
- [ ] Search history

## 🐛 Troubleshooting

### Common Issues

1. **Server not responding**
   - Ensure Memorix server is running on port 8080
   - Check browser console for CORS errors

2. **No memories showing**
   - Default user is "demo-user"
   - Try loading demo data from Operations page
   - Check filters in Memory Explorer

3. **Module errors in console**
   - Ensure you're accessing via `http://localhost:8080` (not `file://`)
   - Modern browser required (ES6 modules support)

## 📄 License

Part of the Memorix project. See main project LICENSE.
