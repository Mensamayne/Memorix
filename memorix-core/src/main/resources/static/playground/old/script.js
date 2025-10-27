// Memorix Playground JavaScript

const API_BASE = 'http://localhost:8080/api/memorix';

// Initialize all event listeners after DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    console.log('[INIT] DOM Content Loaded, setting up event listeners...');
    
    // Tab switching
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;
            
            // Update buttons
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            // Update content
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            document.getElementById(`tab-${tabName}`).classList.add('active');
            
            // Load datasource info when clicking on datasources tab
            if (tabName === 'datasources') {
                loadDataSourceInfo();
            }
        });
    });

    // Save Memory Form
    setupSaveForm();
    
    // Search Form
    setupSearchForm();
    
    // Decay Form
    setupDecayForm();
    
    // Stats Form
    setupStatsForm();
    
    // Load plugin info and show welcome message
    loadPluginInfo();
    loadDataSourceStatus();
    showInfo('👋 Welcome to Memorix Playground! Try loading demo data or create your own memories.');
});

// Save Memory Form Handler
function setupSaveForm() {
    document.getElementById('save-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const data = {
            userId: document.getElementById('save-userId').value,
            content: document.getElementById('save-content').value,
            pluginType: document.getElementById('save-pluginType').value,
            importance: parseFloat(document.getElementById('save-importance').value)
        };
        
        try {
            showLoading();
            const response = await fetch(`${API_BASE}/memories`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            
            const result = await response.json();
            
            // Check if it's an error response (duplicate rejected)
            if (response.status === 409) {
                showError(`❌ Duplicate Rejected: ${result.message}\nExisting: "${result.existingContent}"`);
                return;
            }
            
            // Success - it's a Memory object
            showSuccess('Memory saved successfully!');
            displaySaveResult(result);
            
            // Clear form
            document.getElementById('save-content').value = '';
        } catch (error) {
            showError('Failed to save memory: ' + error.message);
        }
    });
}

// Search Memories Form Handler
function setupSearchForm() {
    document.getElementById('search-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const data = {
            userId: document.getElementById('search-userId').value,
            query: document.getElementById('search-query').value,
            pluginType: document.getElementById('search-pluginType').value,
            maxCount: parseInt(document.getElementById('search-maxCount').value),
            maxTokens: parseInt(document.getElementById('search-maxTokens').value) || null,
            minSimilarity: parseFloat(document.getElementById('search-minSimilarity').value) || null,
            strategy: document.getElementById('search-strategy').value
        };
        
        try {
            showLoading();
            const response = await fetch(`${API_BASE}/memories/search`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            
            const result = await response.json();
            showSuccess(`Found ${result.memories.length} memories!`);
            displaySearchResults(result);
        } catch (error) {
            showError('Search failed: ' + error.message);
        }
    });
}

// Decay Form Handler
function setupDecayForm() {
    document.getElementById('decay-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const data = {
            userId: document.getElementById('decay-userId').value,
            pluginType: document.getElementById('decay-pluginType').value,
            activeSession: document.getElementById('decay-activeSession').checked,
            usedMemoryIds: window.lastSearchResults?.memories?.map(m => m.id) || []
        };
        
        try {
            showLoading();
            const response = await fetch(`${API_BASE}/lifecycle/decay`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            
            const result = await response.json();
            showSuccess('Decay applied!');
            displayDecayResult(result);
        } catch (error) {
            showError('Decay failed: ' + error.message);
        }
    });
}

// Stats Form Handler
function setupStatsForm() {
    console.log('[SETUP] Setting up stats form handler');
    const statsForm = document.getElementById('stats-form');
    console.log('[SETUP] Stats form element:', statsForm);
    
    if (!statsForm) {
        console.error('[SETUP] Stats form not found!');
        return;
    }
    
    statsForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const userId = document.getElementById('stats-userId').value;
        console.log('[STATS] Starting stats request for userId:', userId);
        console.log('[STATS] URL:', `${API_BASE}/stats/${userId}`);
        
        try {
            showLoading();
            console.log('[STATS] Sending fetch request...');
            const response = await fetch(`${API_BASE}/stats/${userId}`);
            console.log('[STATS] Response received:', response.status, response.statusText);
            const stats = await response.json();
            console.log('[STATS] Stats data:', stats);
            showSuccess('Stats loaded!');
            displayStats(stats);
        } catch (error) {
            console.error('[STATS] Error occurred:', error);
            showError('Failed to load stats: ' + error.message);
        }
    });
}

// Display Functions

function displaySaveResult(memory) {
    const html = `
        <div class="message message-success">
            ✅ Memory saved successfully!
        </div>
        <div class="result-item">
            <div class="result-header">
                <span class="result-id">ID: ${memory.id}</span>
                <div class="result-badges">
                    <span class="badge badge-decay">Decay: ${memory.decay}</span>
                    <span class="badge badge-tokens">${memory.tokenCount} tokens</span>
                </div>
            </div>
            <div class="result-content">${escapeHtml(memory.content)}</div>
            <div class="result-meta">
                <div><strong>User:</strong> ${memory.userId}</div>
                <div><strong>Type:</strong> ${memory.type || 'N/A'}</div>
                <div><strong>Importance:</strong> ${memory.importance.toFixed(2)}</div>
                <div><strong>Created:</strong> ${new Date(memory.createdAt).toLocaleString()}</div>
            </div>
        </div>
    `;
    
    document.getElementById('results-container').innerHTML = html;
}

function displaySearchResults(result) {
    window.lastSearchResults = result;  // Store for decay
    
    const metadata = result.metadata;
    const memories = result.memories;
    
    let html = `
        <div class="query-metadata">
            <h3>Query Metadata</h3>
            <div class="metadata-grid">
                <div class="metadata-item">
                    <span class="metadata-value">${metadata.totalFound}</span>
                    <span class="metadata-label">Total Found</span>
                </div>
                <div class="metadata-item">
                    <span class="metadata-value">${metadata.returned}</span>
                    <span class="metadata-label">Returned</span>
                </div>
                <div class="metadata-item">
                    <span class="metadata-value">${metadata.totalTokens}</span>
                    <span class="metadata-label">Total Tokens</span>
                </div>
                <div class="metadata-item">
                    <span class="metadata-value">${metadata.limitReason}</span>
                    <span class="metadata-label">Limited By</span>
                </div>
                <div class="metadata-item">
                    <span class="metadata-value">${metadata.executionTimeMs}ms</span>
                    <span class="metadata-label">Execution Time</span>
                </div>
            </div>
        </div>
    `;
    
    if (memories.length === 0) {
        html += `
            <div class="empty-state">
                <span class="emoji">😕</span>
                <p>No memories found</p>
            </div>
        `;
    } else {
        memories.forEach((memory, index) => {
            html += `
                <div class="result-item">
                    <div class="result-header">
                        <span class="result-id">#${index + 1} - ${memory.id}</span>
                        <div class="result-badges">
                            <span class="badge badge-decay">Decay: ${memory.decay}</span>
                            <span class="badge badge-tokens">${memory.tokenCount} tokens</span>
                        </div>
                    </div>
                    <div class="result-content">${escapeHtml(memory.content)}</div>
                    <div class="result-meta">
                        <div><strong>Importance:</strong> ${memory.importance.toFixed(2)}</div>
                        <div><strong>Created:</strong> ${new Date(memory.createdAt).toLocaleString()}</div>
                    </div>
                </div>
            `;
        });
    }
    
    document.getElementById('results-container').innerHTML = html;
}

function displayDecayResult(result) {
    const html = `
        <div class="message message-success">
            ✅ Lifecycle applied successfully!
        </div>
        <div class="stat-card">
            <div class="metadata-grid">
                <div class="metadata-item">
                    <span class="metadata-value">${result.decayApplied}</span>
                    <span class="metadata-label">Memories Processed</span>
                </div>
                <div class="metadata-item">
                    <span class="metadata-value">${result.memoriesDeleted}</span>
                    <span class="metadata-label">Expired Deleted</span>
                </div>
            </div>
        </div>
        <div class="message message-info">
            💡 Tip: Memories from last search were marked as "used" and reinforced!
        </div>
    `;
    
    document.getElementById('results-container').innerHTML = html;
}

function displayStats(stats) {
    const html = `
        <div class="stat-card">
            <div class="stat-value">${stats.totalMemories}</div>
            <div class="stat-label">Total Memories</div>
        </div>
    `;
    
    document.getElementById('stats-display').innerHTML = html;
    
    // Clear loading state from results container
    document.getElementById('results-container').innerHTML = `
        <div class="empty-state">
            <span class="emoji">📊</span>
            <p>Stats loaded successfully!</p>
        </div>
    `;
}

// Quick Actions

async function loadDemoData() {
    const demoMemories = [
        { content: "User loves pizza margherita with extra basil", type: "USER_PREFERENCE" },
        { content: "User prefers al dente pasta carbonara", type: "USER_PREFERENCE" },
        { content: "User drinks only black coffee, no sugar", type: "USER_PREFERENCE" },
        { content: "User is vegetarian, doesn't eat meat", type: "USER_PREFERENCE" },
        { content: "API endpoint /memories returns user memories", type: "DOCUMENTATION" },
        { content: "Discussed vacation plans for summer in Italy", type: "CONVERSATION" },
    ];
    
    try {
        showLoading();
        let saved = 0;
        
        for (const demo of demoMemories) {
            await fetch(`${API_BASE}/memories`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: 'demo-user',
                    content: demo.content,
                    pluginType: demo.type,
                    importance: 0.7
                })
            });
            saved++;
        }
        
        showSuccess(`✅ Loaded ${saved} demo memories!`);
        
        // Show stats
        const statsResponse = await fetch(`${API_BASE}/stats/demo-user`);
        const stats = await statsResponse.json();
        
        displayStats(stats);
    } catch (error) {
        showError('Failed to load demo data: ' + error.message);
    }
}

async function searchDemo() {
    document.getElementById('search-userId').value = 'demo-user';
    document.getElementById('search-query').value = 'food preferences';
    document.getElementById('search-pluginType').value = 'USER_PREFERENCE';
    
    document.getElementById('search-form').dispatchEvent(new Event('submit'));
}

async function applyDecayDemo() {
    document.getElementById('decay-userId').value = 'demo-user';
    document.getElementById('decay-pluginType').value = 'USER_PREFERENCE';
    
    document.getElementById('decay-form').dispatchEvent(new Event('submit'));
}

async function testDeduplication() {
    const userId = 'demo-user';
    const content = 'User loves pizza margherita';
    
    try {
        showLoading();
        
        // Step 1: Save first memory
        const response1 = await fetch(`${API_BASE}/memories`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: userId,
                content: content,
                pluginType: 'USER_PREFERENCE',
                importance: 0.8
            })
        });
        const memory1 = await response1.json();
        
        // Step 2: Save exact duplicate (should MERGE)
        const response2 = await fetch(`${API_BASE}/memories`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: userId,
                content: content,  // Same content!
                pluginType: 'USER_PREFERENCE',
                importance: 0.8
            })
        });
        const memory2 = await response2.json();
        
        // Display results
        const resultsHtml = `
            <div class="dedup-demo">
                <h3>🔄 Deduplication Demo Results</h3>
                
                <div class="dedup-step">
                    <h4>Step 1: First Save</h4>
                    <div class="memory-card">
                        <div class="memory-header">
                            <span class="memory-id">ID: ${memory1.id}</span>
                            <span class="badge badge-success">NEW</span>
                        </div>
                        <div class="memory-content">${memory1.content}</div>
                        <div class="memory-meta">
                            <span>Decay: <strong>${memory1.decay}</strong></span>
                            <span>Tokens: ${memory1.tokenCount}</span>
                        </div>
                    </div>
                </div>
                
                <div class="dedup-step">
                    <h4>Step 2: Duplicate Detected!</h4>
                    <div class="memory-card ${memory1.id === memory2.id ? 'merged' : ''}">
                        <div class="memory-header">
                            <span class="memory-id">ID: ${memory2.id}</span>
                            <span class="badge ${memory1.id === memory2.id ? 'badge-warning' : 'badge-success'}">
                                ${memory1.id === memory2.id ? 'MERGED ✅' : 'NEW'}
                            </span>
                        </div>
                        <div class="memory-content">${memory2.content}</div>
                        <div class="memory-meta">
                            <span>Decay: <strong>${memory2.decay}</strong> 
                                ${memory1.id === memory2.id ? `(+${memory2.decay - memory1.decay})` : ''}
                            </span>
                            <span>Tokens: ${memory2.tokenCount}</span>
                        </div>
                    </div>
                </div>
                
                <div class="dedup-summary">
                    ${memory1.id === memory2.id ? 
                        `<p class="success">✅ <strong>Deduplication worked!</strong> Same ID returned, decay reinforced from ${memory1.decay} to ${memory2.decay}</p>` :
                        `<p class="warning">⚠️ Different IDs - deduplication may be disabled for this plugin</p>`
                    }
                </div>
            </div>
        `;
        
        document.getElementById('results-container').innerHTML = resultsHtml;
        
        if (memory1.id === memory2.id) {
            showSuccess(`✅ Deduplication MERGE successful! Decay: ${memory1.decay} → ${memory2.decay}`);
        } else {
            showInfo('ℹ️ Created separate memories (deduplication may be disabled)');
        }
    } catch (error) {
        showError('Failed to test deduplication: ' + error.message);
    }
}

async function clearAll() {
    const userId = 'demo-user';
    
    if (!confirm(`Are you sure you want to clear all memories for "${userId}"? This cannot be undone.`)) {
        return;
    }
    
    try {
        showLoading();
        console.log('[CLEAR] Sending DELETE request to:', `${API_BASE}/memories/${userId}`);
        
        const response = await fetch(`${API_BASE}/memories/${userId}`, {
            method: 'DELETE'
        });
        
        console.log('[CLEAR] Response received:', response.status, response.statusText);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const result = await response.json();
        console.log('[CLEAR] Result:', result);
        
        showSuccess(`✅ Cleared ${result.deletedCount} memories for ${result.userId}`);
        
        // Refresh stats
        document.getElementById('stats-userId').value = userId;
        document.getElementById('stats-form').dispatchEvent(new Event('submit'));
    } catch (error) {
        console.error('[CLEAR] Error occurred:', error);
        showError('Failed to clear memories: ' + error.message);
    }
}

// Load plugin info on page load
async function loadPluginInfo() {
    try {
        const response = await fetch(`${API_BASE}/plugins`);
        const plugins = await response.json();
        
        let html = '';
        for (const pluginType of plugins) {
            const infoResponse = await fetch(`${API_BASE}/plugins/${pluginType}`);
            const info = await infoResponse.json();
            
            // Handle missing decayConfig gracefully
            const decayConfig = info.decayConfig || {};
            const strategyName = decayConfig.strategyClassName 
                ? decayConfig.strategyClassName.split('.').pop() 
                : 'Unknown';
            
            // Handle deduplication config
            const dedupConfig = info.deduplicationConfig || {};
            const dedupEnabled = dedupConfig.enabled || false;
            const dedupStrategy = dedupConfig.strategy || 'N/A';
            const reinforceOnMerge = dedupConfig.reinforceOnMerge !== undefined ? dedupConfig.reinforceOnMerge : 'N/A';
            const semanticEnabled = dedupConfig.semanticEnabled || false;
            const semanticThreshold = dedupConfig.semanticThreshold || 'N/A';
            
            html += `
                <div class="plugin-card">
                    <h4>${pluginType}</h4>
                    <div class="plugin-detail">
                        <strong>Strategy:</strong> ${strategyName}
                    </div>
                    <div class="plugin-detail">
                        <strong>Initial Decay:</strong> ${decayConfig.initialDecay || 'N/A'}
                    </div>
                    <div class="plugin-detail">
                        <strong>Decay Range:</strong> ${decayConfig.minDecay || 0} - ${decayConfig.maxDecay || 0}
                    </div>
                    <div class="plugin-detail">
                        <strong>Auto-Delete:</strong> ${decayConfig.autoDelete ? '✅' : '❌'}
                    </div>
                    <div class="plugin-detail">
                        <strong>Default Limit:</strong> ${info.defaultQueryLimit?.maxCount || 20} memories, ${info.defaultQueryLimit?.maxTokens || '∞'} tokens
                    </div>
                    <div class="plugin-detail">
                        <strong>Deduplication:</strong> ${dedupEnabled ? `✅ ${dedupStrategy}` : '❌ Disabled'}
                    </div>
                    ${dedupEnabled && dedupStrategy === 'MERGE' ? `<div class="plugin-detail"><strong>Reinforce on Merge:</strong> ${reinforceOnMerge === true ? '✅' : reinforceOnMerge === false ? '❌' : reinforceOnMerge}</div>` : ''}
                    ${dedupEnabled && semanticEnabled ? `<div class="plugin-detail"><strong>Semantic Detection:</strong> ✅ (threshold: ${semanticThreshold})</div>` : ''}
                </div>
            `;
        }
        
        document.getElementById('plugin-info').innerHTML = html;
    } catch (error) {
        console.error('Failed to load plugin info:', error);
    }
}

// Message helpers
function showSuccess(message) {
    showMessage(message, 'success');
}

function showError(message) {
    showMessage(message, 'error');
}

function showInfo(message) {
    showMessage(message, 'info');
}

function showMessage(message, type) {
    const resultsContainer = document.getElementById('results-container');
    const messageHtml = `<div class="message message-${type}">${message}</div>`;
    
    // Prepend message
    resultsContainer.insertAdjacentHTML('afterbegin', messageHtml);
    
    // Remove after 5 seconds
    setTimeout(() => {
        const msg = resultsContainer.querySelector('.message');
        if (msg) msg.remove();
    }, 5000);
}

function showLoading() {
    const resultsContainer = document.getElementById('results-container');
    resultsContainer.innerHTML = `
        <div class="empty-state">
            <div class="loading"></div>
            <p style="margin-top: 20px;">Processing...</p>
        </div>
    `;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ====================
// Multi-DataSource Support
// ====================

async function loadDataSourceStatus() {
    try {
        const response = await fetch(`${API_BASE}/datasources`);
        const info = await response.json();
        
        const statusEl = document.getElementById('multi-ds-status');
        const footerEl = document.getElementById('datasource-status');
        
        if (info.enabled) {
            statusEl.innerHTML = `
                <span class="status-dot status-green"></span>
                Multi-DS: ${info.datasources.length} databases
            `;
            footerEl.textContent = `Multi-DataSource: ${info.datasources.join(', ')}`;
        } else {
            statusEl.innerHTML = `
                <span class="status-dot status-blue"></span>
                Single-DB Mode
            `;
            footerEl.textContent = 'Single Database Mode';
        }
        
        // Store for later use
        window.datasourceInfo = info;
        
    } catch (error) {
        console.error('Failed to load datasource status:', error);
        document.getElementById('multi-ds-status').innerHTML = `
            <span class="status-dot status-red"></span>
            Multi-DS: Error
        `;
    }
}

async function loadDataSourceInfo() {
    try {
        showLoading();
        
        const [infoResponse, statsResponse] = await Promise.all([
            fetch(`${API_BASE}/datasources`),
            fetch(`${API_BASE}/datasources/stats`)
        ]);
        
        const info = await infoResponse.json();
        const stats = await statsResponse.json();
        
        let html = '<div class="datasource-browser">';
        
        // Header
        html += '<div class="ds-header">';
        html += `<h4>🗄️ Multi-DataSource Configuration</h4>`;
        html += `<p><strong>Status:</strong> ${info.enabled ? '✅ Enabled' : '❌ Disabled (Single DB)'}</p>`;
        html += `<p><strong>Configured Datasources:</strong> ${info.datasources.length}</p>`;
        html += '</div>';
        
        // DataSource Cards
        for (const dsName of info.datasources) {
            const dsStats = stats[dsName];
            
            html += '<div class="ds-card">';
            html += `<h5>📦 ${dsName}</h5>`;
            
            if (dsStats) {
                html += '<div class="ds-plugins">';
                html += `<p><strong>Plugins using this database:</strong></p>`;
                html += '<ul>';
                for (const plugin of dsStats.pluginTypes) {
                    html += `<li><span class="plugin-badge">${plugin}</span></li>`;
                }
                html += '</ul>';
                html += '</div>';
            } else {
                html += '<p class="text-muted">No plugins configured</p>';
            }
            
            html += '</div>';
        }
        
        // Plugin to DataSource Mapping Table
        html += '<div class="ds-mapping">';
        html += '<h4>🔌 Plugin → DataSource Mapping</h4>';
        html += '<table class="mapping-table">';
        html += '<thead><tr><th>Plugin Type</th><th>DataSource</th></tr></thead>';
        html += '<tbody>';
        
        for (const [pluginType, dsName] of Object.entries(info.pluginDataSourceMapping)) {
            const badge = dsName === 'default' ? 'badge-default' : 'badge-custom';
            html += `<tr>`;
            html += `<td><strong>${pluginType}</strong></td>`;
            html += `<td><span class="ds-badge ${badge}">${dsName}</span></td>`;
            html += `</tr>`;
        }
        
        html += '</tbody></table>';
        html += '</div>';
        
        html += '</div>';
        
        document.getElementById('results-container').innerHTML = html;
        
    } catch (error) {
        showError('Failed to load datasource info: ' + error.message);
    }
}


