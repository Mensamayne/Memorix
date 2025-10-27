/**
 * Main Application Entry Point
 * Handles routing and page initialization
 */

import { AppState } from './state.js';
import { API } from './api.js';
import { toast } from './components/toast.js';
import { renderDashboard } from './pages/dashboard.js';
import { renderMemories } from './pages/memories.js';
import { renderOperations } from './pages/operations.js';
import { renderSearch } from './pages/search.js';
import { renderLifecycle } from './pages/lifecycle.js';
import { renderDatabases } from './pages/databases.js';

// Page renderers
const PAGES = {
    dashboard: renderDashboard,
    memories: renderMemories,
    operations: renderOperations,
    search: renderSearch,
    lifecycle: renderLifecycle,
    databases: renderDatabases
};

class App {
    constructor() {
        this.mainContent = document.getElementById('app-main');
        this.currentPage = 'dashboard';
    }
    
    async init() {
        console.log('[App] Initializing Memorix Playground...');
        
        // Setup navigation
        this.setupNavigation();
        
        // Load initial data
        await this.loadInitialData();
        
        // Navigate to initial page
        this.navigateTo('dashboard');
        
        // Subscribe to state changes
        AppState.subscribe((state, changedKeys) => {
            console.log('[App] State changed:', changedKeys);
        });
        
        console.log('[App] Initialization complete');
        
        // Show welcome message
        toast.info('Welcome to Memorix Playground! Explore the memory management system.', 'Welcome');
    }
    
    setupNavigation() {
        // Navigation links
        const navLinks = document.querySelectorAll('.nav-link');
        navLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const page = link.dataset.page;
                if (page) {
                    this.navigateTo(page);
                }
            });
        });
        
        // Brand link
        const brandLink = document.querySelector('.navbar-brand');
        if (brandLink) {
            brandLink.addEventListener('click', (e) => {
                e.preventDefault();
                this.navigateTo('dashboard');
            });
        }
    }
    
    async loadInitialData() {
        try {
            // Load plugins
            const plugins = await API.getPlugins();
            AppState.setState({ plugins });
            
            // Load plugin configs
            const pluginConfigs = {};
            for (const pluginType of plugins) {
                try {
                    pluginConfigs[pluginType] = await API.getPluginConfig(pluginType);
                } catch (error) {
                    console.warn(`[App] Failed to load config for ${pluginType}:`, error);
                }
            }
            AppState.setState({ pluginConfigs });
            
            // Load datasource info
            try {
                const datasources = await API.getDatasources();
                AppState.setState({ datasources });
                
                // Update status indicator
                this.updateDatasourceStatus(datasources);
            } catch (error) {
                console.warn('[App] Failed to load datasources:', error);
            }
            
        } catch (error) {
            console.error('[App] Failed to load initial data:', error);
            toast.error('Failed to load initial configuration', 'Error');
        }
    }
    
    navigateTo(page) {
        if (!PAGES[page]) {
            console.error(`[App] Unknown page: ${page}`);
            return;
        }
        
        console.log(`[App] Navigating to: ${page}`);
        
        // Update active nav link
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.toggle('active', link.dataset.page === page);
        });
        
        // Update state
        this.currentPage = page;
        AppState.setState({ currentPage: page });
        
        // Show loading
        this.mainContent.innerHTML = `
            <div style="display: flex; align-items: center; justify-content: center; min-height: 400px;">
                <div style="text-align: center;">
                    <div class="spinner" style="width: 40px; height: 40px; border-width: 4px;"></div>
                    <p style="margin-top: var(--spacing-md); color: var(--text-secondary);">Loading...</p>
                </div>
            </div>
        `;
        
        // Render page
        setTimeout(async () => {
            try {
                await PAGES[page](this.mainContent);
            } catch (error) {
                console.error(`[App] Error rendering ${page}:`, error);
                this.mainContent.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">⚠️</div>
                        <div class="empty-state-title">Failed to load page</div>
                        <div class="empty-state-message">${error.message}</div>
                    </div>
                `;
            }
        }, 100);
    }
    
    updateDatasourceStatus(datasources) {
        const statusIndicator = document.getElementById('db-status');
        if (!statusIndicator) return;
        
        const statusDot = statusIndicator.querySelector('.status-dot');
        const statusText = statusIndicator.querySelector('span:last-child');
        
        if (datasources.enabled) {
            statusDot.className = 'status-dot online';
            statusText.textContent = `${datasources.datasources.length} DB`;
        } else {
            statusDot.className = 'status-dot online';
            statusText.textContent = 'PostgreSQL';
        }
    }
}

// Initialize app when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        const app = new App();
        app.init();
    });
} else {
    const app = new App();
    app.init();
}

