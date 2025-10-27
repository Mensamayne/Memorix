/**
 * Databases Page
 * Multi-datasource browser and configuration
 */

import { API } from '../api.js';
import { AppState } from '../state.js';
import { toast } from '../components/toast.js';

export async function renderDatabases(container) {
    container.innerHTML = `
        <div class="databases-page">
            <!-- Page Header -->
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-xl);">
                <div>
                    <h1 style="margin-bottom: var(--spacing-sm);">🗄️ Database Browser</h1>
                    <p style="color: var(--text-secondary);">Multi-datasource configuration and stats</p>
                </div>
                <button class="btn btn-primary" id="refresh-datasources">
                    🔄 Refresh
                </button>
            </div>
            
            <!-- Loading State -->
            <div id="datasources-content">
                <div style="text-align: center; padding: var(--spacing-2xl);">
                    <div class="spinner" style="width: 40px; height: 40px; border-width: 4px;"></div>
                    <p style="margin-top: var(--spacing-md); color: var(--text-secondary);">Loading datasource information...</p>
                </div>
            </div>
        </div>
    `;
    
    // Setup refresh button
    const refreshBtn = container.querySelector('#refresh-datasources');
    refreshBtn.addEventListener('click', () => loadDatasources(container));
    
    // Load datasources
    await loadDatasources(container);
}

async function loadDatasources(container) {
    const contentDiv = container.querySelector('#datasources-content');
    
    // Show loading
    contentDiv.innerHTML = `
        <div style="text-align: center; padding: var(--spacing-2xl);">
            <div class="spinner" style="width: 40px; height: 40px; border-width: 4px;"></div>
            <p style="margin-top: var(--spacing-md); color: var(--text-secondary);">Loading...</p>
        </div>
    `;
    
    try {
        const [datasources, stats] = await Promise.all([
            API.getDatasources(),
            API.getDatasourceStats()
        ]);
        
        displayDatasources(contentDiv, datasources, stats);
        
    } catch (error) {
        console.error('[Databases] Failed to load datasources:', error);
        contentDiv.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">⚠️</div>
                <div class="empty-state-title">Failed to Load Datasources</div>
                <div class="empty-state-message">${error.message}</div>
            </div>
        `;
        toast.error('Failed to load datasource information');
    }
}

function displayDatasources(container, datasources, stats) {
    container.innerHTML = '';
    
    // Overview Card
    const overviewCard = document.createElement('div');
    overviewCard.className = 'card';
    overviewCard.style.marginBottom = 'var(--spacing-xl)';
    overviewCard.innerHTML = `
        <div class="card-header">
            <h2 class="card-title">📊 Overview</h2>
        </div>
        <div class="card-body">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: var(--spacing-md);">
                <div style="text-align: center; padding: var(--spacing-lg); background: var(--bg-tertiary); border-radius: var(--radius-lg);">
                    <div style="font-size: 2rem; font-weight: 700; color: var(--primary-500); margin-bottom: var(--spacing-sm);">
                        ${datasources.enabled ? '✅' : '❌'}
                    </div>
                    <div class="text-sm text-muted">Multi-DataSource</div>
                    <div class="text-sm" style="color: var(--text-primary); margin-top: var(--spacing-xs);">
                        ${datasources.enabled ? 'Enabled' : 'Disabled'}
                    </div>
                </div>
                
                <div style="text-align: center; padding: var(--spacing-lg); background: var(--bg-tertiary); border-radius: var(--radius-lg);">
                    <div style="font-size: 2rem; font-weight: 700; color: var(--primary-500); margin-bottom: var(--spacing-sm);">
                        ${datasources.datasources.length}
                    </div>
                    <div class="text-sm text-muted">Configured</div>
                    <div class="text-sm" style="color: var(--text-primary); margin-top: var(--spacing-xs);">
                        Database${datasources.datasources.length !== 1 ? 's' : ''}
                    </div>
                </div>
                
                <div style="text-align: center; padding: var(--spacing-lg); background: var(--bg-tertiary); border-radius: var(--radius-lg);">
                    <div style="font-size: 2rem; font-weight: 700; color: var(--primary-500); margin-bottom: var(--spacing-sm);">
                        ${Object.keys(datasources.pluginDataSourceMapping).length}
                    </div>
                    <div class="text-sm text-muted">Plugin</div>
                    <div class="text-sm" style="color: var(--text-primary); margin-top: var(--spacing-xs);">
                        Mappings
                    </div>
                </div>
            </div>
        </div>
    `;
    container.appendChild(overviewCard);
    
    // Datasource Cards
    if (datasources.datasources.length > 0) {
        const datasourcesSection = document.createElement('div');
        datasourcesSection.style.marginBottom = 'var(--spacing-xl)';
        datasourcesSection.innerHTML = '<h2 style="margin-bottom: var(--spacing-md);">🗄️ Datasources</h2>';
        
        const datasourcesGrid = document.createElement('div');
        datasourcesGrid.style.display = 'grid';
        datasourcesGrid.style.gridTemplateColumns = 'repeat(auto-fit, minmax(300px, 1fr))';
        datasourcesGrid.style.gap = 'var(--spacing-md)';
        
        datasources.datasources.forEach(dsName => {
            const dsStats = stats[dsName];
            
            const card = document.createElement('div');
            card.className = 'card';
            card.innerHTML = `
                <div class="card-header">
                    <h3 class="card-title">📦 ${dsName}</h3>
                </div>
                <div class="card-body">
                    ${dsStats ? `
                        <div style="margin-bottom: var(--spacing-md);">
                            <div class="text-sm text-muted" style="margin-bottom: var(--spacing-sm);">Plugins using this database:</div>
                            <div style="display: flex; flex-wrap: wrap; gap: var(--spacing-xs);">
                                ${dsStats.pluginTypes.map(plugin => `
                                    <span class="badge badge-primary">${plugin}</span>
                                `).join('')}
                            </div>
                        </div>
                    ` : `
                        <div class="text-sm text-muted">No plugins configured for this datasource</div>
                    `}
                </div>
            `;
            
            datasourcesGrid.appendChild(card);
        });
        
        datasourcesSection.appendChild(datasourcesGrid);
        container.appendChild(datasourcesSection);
    }
    
    // Plugin Mapping Table
    const mappingCard = document.createElement('div');
    mappingCard.className = 'card';
    mappingCard.innerHTML = `
        <div class="card-header">
            <h2 class="card-title">🔌 Plugin → DataSource Mapping</h2>
        </div>
        <div class="card-body">
            <table class="table">
                <thead>
                    <tr>
                        <th>Plugin Type</th>
                        <th>DataSource</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${Object.entries(datasources.pluginDataSourceMapping).map(([plugin, ds]) => `
                        <tr>
                            <td><strong>${plugin}</strong></td>
                            <td><code>${ds}</code></td>
                            <td>
                                <span class="badge ${ds === 'default' ? 'badge-secondary' : 'badge-success'}">
                                    ${ds === 'default' ? 'Default' : 'Custom'}
                                </span>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
    container.appendChild(mappingCard);
    
    // Configuration Info
    if (!datasources.enabled) {
        const infoCard = document.createElement('div');
        infoCard.className = 'card';
        infoCard.style.marginTop = 'var(--spacing-xl)';
        infoCard.style.borderColor = 'var(--info-500)';
        infoCard.innerHTML = `
            <div class="card-body">
                <div style="display: flex; gap: var(--spacing-md);">
                    <div style="font-size: 2rem;">ℹ️</div>
                    <div>
                        <h4 style="color: var(--info-500); margin-bottom: var(--spacing-sm);">Single Database Mode</h4>
                        <p style="color: var(--text-secondary); margin-bottom: var(--spacing-sm);">
                            Multi-datasource is currently disabled. All plugins use the default database.
                        </p>
                        <p style="color: var(--text-tertiary); font-size: 0.875rem;">
                            To enable multi-datasource support, configure it in your application.yml and restart the server.
                        </p>
                    </div>
                </div>
            </div>
        `;
        container.appendChild(infoCard);
    }
}

