/**
 * Lifecycle Page
 * Decay management and memory lifecycle operations
 */

import { API } from '../api.js';
import { AppState } from '../state.js';
import { toast } from '../components/toast.js';

export async function renderLifecycle(container) {
    container.innerHTML = `
        <div class="lifecycle-page">
            <!-- Page Header -->
            <div style="margin-bottom: var(--spacing-xl);">
                <h1 style="margin-bottom: var(--spacing-sm);">⏱️ Memory Lifecycle</h1>
                <p style="color: var(--text-secondary);">Manage memory decay and lifecycle operations</p>
            </div>
            
            <!-- Apply Decay -->
            <div class="card" style="margin-bottom: var(--spacing-xl);">
                <div class="card-header">
                    <h2 class="card-title">🔄 Apply Decay</h2>
                </div>
                <div class="card-body">
                    <form id="decay-form">
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">User ID</label>
                                <input type="text" class="form-input" id="decay-userId" value="demo-user" required>
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Plugin Type</label>
                                <select class="form-select" id="decay-pluginType" required>
                                </select>
                            </div>
                        </div>
                        
                        <div class="form-group">
                            <label style="display: flex; align-items: center; gap: var(--spacing-sm); cursor: pointer;">
                                <input type="checkbox" class="form-checkbox" id="decay-activeSession" checked>
                                <span class="form-label" style="margin-bottom: 0;">Active Session (decay used memories less)</span>
                            </label>
                        </div>
                        
                        <div style="padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-md); font-size: 0.875rem; color: var(--text-secondary); margin-bottom: var(--spacing-md);">
                            💡 <strong>How it works:</strong><br>
                            - All memories will have decay applied based on their strategy<br>
                            - Memories with decay below threshold will be auto-deleted (if enabled)<br>
                            - Active session mode applies gentler decay to recently used memories
                        </div>
                        
                        <button type="submit" class="btn btn-warning" style="width: 100%;">
                            ⏱️ Apply Decay & Cleanup
                        </button>
                    </form>
                </div>
            </div>
            
            <!-- Decay Strategies Info -->
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">📚 Decay Strategies</h2>
                </div>
                <div class="card-body">
                    <div id="decay-strategies">
                        <!-- Will be populated with plugin decay configs -->
                    </div>
                </div>
            </div>
            
            <!-- Result Display -->
            <div class="card" style="margin-top: var(--spacing-xl);">
                <div class="card-header">
                    <h2 class="card-title">📋 Result</h2>
                </div>
                <div class="card-body">
                    <div id="decay-result">
                        <div class="empty-state">
                            <div class="empty-state-icon">⏱️</div>
                            <div class="empty-state-message">Apply decay to see results</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Populate plugin types
    const pluginSelect = container.querySelector('#decay-pluginType');
    AppState.plugins.forEach(plugin => {
        const option = document.createElement('option');
        option.value = plugin;
        option.textContent = plugin;
        pluginSelect.appendChild(option);
    });
    
    // Load decay strategies
    loadDecayStrategies(container);
    
    // Setup form listener
    const decayForm = container.querySelector('#decay-form');
    decayForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await applyDecay(container);
    });
}

function loadDecayStrategies(container) {
    const strategiesDiv = container.querySelector('#decay-strategies');
    const pluginConfigs = AppState.pluginConfigs;
    
    strategiesDiv.innerHTML = AppState.plugins.map(pluginType => {
        const config = pluginConfigs[pluginType] || {};
        const decayConfig = config.decayConfig || {};
        
        const strategyName = decayConfig.strategyClassName 
            ? decayConfig.strategyClassName.split('.').pop() 
            : 'Unknown';
        
        return `
            <div class="card" style="background: var(--bg-tertiary); margin-bottom: var(--spacing-md);">
                <div class="card-body">
                    <h4 style="color: var(--primary-500); margin-bottom: var(--spacing-md);">${pluginType}</h4>
                    
                    <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--spacing-md); font-size: 0.875rem;">
                        <div>
                            <div class="text-muted">Strategy</div>
                            <div><strong>${strategyName}</strong></div>
                        </div>
                        <div>
                            <div class="text-muted">Initial Decay</div>
                            <div><strong>${decayConfig.initialDecay || 'N/A'}</strong></div>
                        </div>
                        <div>
                            <div class="text-muted">Decay Range</div>
                            <div><strong>${decayConfig.minDecay || 0} - ${decayConfig.maxDecay || 0}</strong></div>
                        </div>
                        <div>
                            <div class="text-muted">Auto-Delete</div>
                            <div><strong>${decayConfig.autoDelete ? '✅ Enabled' : '❌ Disabled'}</strong></div>
                        </div>
                        <div>
                            <div class="text-muted">Decay Factor (not used)</div>
                            <div><strong>${decayConfig.decayFactorNotUsed || 'N/A'}</strong></div>
                        </div>
                        <div>
                            <div class="text-muted">Decay Factor (used)</div>
                            <div><strong>${decayConfig.decayFactorUsed || 'N/A'}</strong></div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

async function applyDecay(container) {
    const resultDiv = container.querySelector('#decay-result');
    const submitBtn = container.querySelector('button[type="submit"]');
    
    // Show loading
    resultDiv.innerHTML = `
        <div style="text-align: center; padding: var(--spacing-xl);">
            <div class="spinner" style="width: 40px; height: 40px; border-width: 4px;"></div>
            <p style="margin-top: var(--spacing-md); color: var(--text-secondary);">Applying decay...</p>
        </div>
    `;
    
    submitBtn.disabled = true;
    submitBtn.textContent = 'Processing...';
    
    try {
        const request = {
            userId: container.querySelector('#decay-userId').value,
            pluginType: container.querySelector('#decay-pluginType').value,
            activeSession: container.querySelector('#decay-activeSession').checked,
            usedMemoryIds: []  // Could be enhanced to mark specific memories as used
        };
        
        const result = await API.applyDecay(request);
        
        resultDiv.innerHTML = `
            <div style="padding: var(--spacing-lg);">
                <div style="text-align: center; margin-bottom: var(--spacing-xl);">
                    <div style="font-size: 3rem; margin-bottom: var(--spacing-md);">✅</div>
                    <h3 style="color: var(--success-500); margin-bottom: var(--spacing-sm);">Decay Applied Successfully</h3>
                </div>
                
                <div class="stat-grid">
                    <div class="stat-card">
                        <div class="stat-value">${result.decayApplied}</div>
                        <div class="stat-label">Memories Processed</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-value" style="color: var(--danger-500);">${result.memoriesDeleted}</div>
                        <div class="stat-label">Expired & Deleted</div>
                    </div>
                </div>
                
                <div style="margin-top: var(--spacing-xl); padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-md); font-size: 0.875rem; color: var(--text-secondary);">
                    💡 Decay has been applied to all memories. Memories below the threshold were automatically deleted.
                </div>
            </div>
        `;
        
        toast.success(`Processed ${result.decayApplied} memories, deleted ${result.memoriesDeleted}`);
        
    } catch (error) {
        console.error('[Lifecycle] Decay failed:', error);
        resultDiv.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">⚠️</div>
                <div class="empty-state-title">Failed to Apply Decay</div>
                <div class="empty-state-message">${error.message}</div>
            </div>
        `;
        toast.error('Failed to apply decay: ' + error.message);
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = '⏱️ Apply Decay & Cleanup';
    }
}

