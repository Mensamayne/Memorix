/**
 * Dashboard Page
 * Overview with statistics and recent memories
 */

import { API } from '../api.js';
import { AppState } from '../state.js';
import { toast } from '../components/toast.js';
import { createMemoryCard } from '../components/memory-card.js';

export async function renderDashboard(container) {
    container.innerHTML = `
        <div class="dashboard">
            <!-- Page Header -->
            <div style="margin-bottom: var(--spacing-xl);">
                <h1 style="margin-bottom: var(--spacing-sm);">Dashboard</h1>
                <p style="color: var(--text-secondary);">Overview of your Memorix system</p>
            </div>
            
            <!-- Stats Grid -->
            <div class="stat-grid" id="stats-grid">
                <div class="stat-card skeleton" style="height: 120px;"></div>
                <div class="stat-card skeleton" style="height: 120px;"></div>
                <div class="stat-card skeleton" style="height: 120px;"></div>
                <div class="stat-card skeleton" style="height: 120px;"></div>
            </div>
            
            <!-- Plugin Configuration -->
            <div class="card" style="margin-bottom: var(--spacing-xl);">
                <div class="card-header">
                    <h2 class="card-title">🔌 Plugin Configuration</h2>
                </div>
                <div class="card-body">
                    <div id="plugin-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: var(--spacing-md);">
                        <div class="skeleton" style="height: 200px;"></div>
                        <div class="skeleton" style="height: 200px;"></div>
                        <div class="skeleton" style="height: 200px;"></div>
                    </div>
                </div>
            </div>
            
            <!-- Recent Memories -->
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">📋 Recent Memories</h2>
                    <button class="btn btn-sm btn-primary" id="view-all-memories">
                        View All →
                    </button>
                </div>
                <div class="card-body">
                    <div id="recent-memories">
                        <div class="skeleton" style="height: 100px; margin-bottom: var(--spacing-md);"></div>
                        <div class="skeleton" style="height: 100px;"></div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Load data
    await Promise.all([
        loadStats(container),
        loadPlugins(container),
        loadRecentMemories(container)
    ]);
    
    // Setup event listeners
    const viewAllBtn = container.querySelector('#view-all-memories');
    if (viewAllBtn) {
        viewAllBtn.addEventListener('click', () => {
            // Navigate to memories page
            const navLink = document.querySelector('.nav-link[data-page="memories"]');
            if (navLink) navLink.click();
        });
    }
}

async function loadStats(container) {
    const statsGrid = container.querySelector('#stats-grid');
    
    try {
        // Get stats for demo-user
        const stats = await API.getStats('demo-user');
        
        // Get total across all plugin types
        const totalMemories = stats.totalMemories || 0;
        
        // Get datasource info
        const datasources = AppState.datasources;
        const dbCount = datasources?.enabled ? datasources.datasources.length : 1;
        
        // Get plugin count
        const pluginCount = AppState.plugins.length;
        
        statsGrid.innerHTML = `
            <div class="stat-card">
                <div class="stat-value">${totalMemories}</div>
                <div class="stat-label">Total Memories</div>
            </div>
            <div class="stat-card">
                <div class="stat-value">${pluginCount}</div>
                <div class="stat-label">Plugin Types</div>
            </div>
            <div class="stat-card">
                <div class="stat-value">${dbCount}</div>
                <div class="stat-label">Database${dbCount > 1 ? 's' : ''}</div>
            </div>
            <div class="stat-card">
                <div class="stat-value" style="color: var(--success-500);">✓</div>
                <div class="stat-label">System Status</div>
            </div>
        `;
        
    } catch (error) {
        console.error('[Dashboard] Failed to load stats:', error);
        statsGrid.innerHTML = `
            <div class="empty-state">
                <p style="color: var(--danger-500);">Failed to load statistics</p>
            </div>
        `;
    }
}

async function loadPlugins(container) {
    const pluginGrid = container.querySelector('#plugin-grid');
    
    try {
        const plugins = AppState.plugins;
        const pluginConfigs = AppState.pluginConfigs;
        
        if (plugins.length === 0) {
            pluginGrid.innerHTML = '<p style="color: var(--text-tertiary);">No plugins configured</p>';
            return;
        }
        
        pluginGrid.innerHTML = plugins.map(pluginType => {
            const config = pluginConfigs[pluginType] || {};
            const decayConfig = config.decayConfig || {};
            const dedupConfig = config.deduplicationConfig || {};
            
            const strategyName = decayConfig.strategyClassName 
                ? decayConfig.strategyClassName.split('.').pop() 
                : 'Unknown';
            
            return `
                <div class="card" style="background: var(--bg-tertiary);">
                    <div class="card-body">
                        <h4 style="color: var(--primary-500); margin-bottom: var(--spacing-md);">${pluginType}</h4>
                        <div style="display: flex; flex-direction: column; gap: var(--spacing-sm); font-size: 0.875rem;">
                            <div style="color: var(--text-secondary);">
                                <strong>Strategy:</strong> ${strategyName}
                            </div>
                            <div style="color: var(--text-secondary);">
                                <strong>Decay Range:</strong> ${decayConfig.minDecay || 0} - ${decayConfig.maxDecay || 0}
                            </div>
                            <div style="color: var(--text-secondary);">
                                <strong>Auto-Delete:</strong> ${decayConfig.autoDelete ? '✅' : '❌'}
                            </div>
                            <div style="color: var(--text-secondary);">
                                <strong>Deduplication:</strong> ${dedupConfig.enabled ? `✅ ${dedupConfig.strategy}` : '❌'}
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
    } catch (error) {
        console.error('[Dashboard] Failed to load plugins:', error);
        pluginGrid.innerHTML = '<p style="color: var(--danger-500);">Failed to load plugin configuration</p>';
    }
}

async function loadRecentMemories(container) {
    const recentContainer = container.querySelector('#recent-memories');
    
    try {
        // Get recent memories
        const result = await API.getMemories({
            userId: 'demo-user',
            pluginType: 'USER_PREFERENCE',
            perPage: 5,
            sortBy: 'createdAt',
            sortOrder: 'desc'
        });
        
        const memories = result.memories || [];
        
        if (memories.length === 0) {
            recentContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📭</div>
                    <div class="empty-state-title">No memories yet</div>
                    <div class="empty-state-message">Create your first memory to get started</div>
                </div>
            `;
            return;
        }
        
        // Show only first 5 memories
        const recentMemories = memories.slice(0, 5);
        
        recentContainer.innerHTML = '';
        recentMemories.forEach(memory => {
            const card = createMemoryCard(memory, {
                showActions: false
            });
            recentContainer.appendChild(card);
        });
        
    } catch (error) {
        console.error('[Dashboard] Failed to load recent memories:', error);
        recentContainer.innerHTML = `
            <div class="empty-state">
                <p style="color: var(--danger-500);">Failed to load recent memories</p>
            </div>
        `;
    }
}

