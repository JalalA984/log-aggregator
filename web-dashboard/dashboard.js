// Configuration
const API_BASE = 'http://localhost:8080';  // API Gateway
let currentPage = 0;
const pageSize = 20;
let hasMoreLogs = true;
let currentSearchParams = {};

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    loadStats();
    loadAllLogs();
    loadSources();
});

// Load statistics
async function loadStats() {
    try {
        const response = await fetch(`${API_BASE}/query/stats?hours=24`);
        const data = await response.json();
        
        if (data.success) {
            const stats = data.statistics;
            document.getElementById('infoCount').textContent = stats.infoCount || 0;
            document.getElementById('warnCount').textContent = stats.warnCount || 0;
            document.getElementById('errorCount').textContent = stats.errorCount || 0;
            document.getElementById('debugCount').textContent = stats.debugCount || 0;
            
            console.log('✅ Stats updated:', stats);
        }
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

// Load all logs (empty search)
async function loadAllLogs() {
    currentPage = 0;
    hasMoreLogs = true;
    await searchLogsInternal({query: "", level: "", sourceApp: "", page: 0, size: pageSize});
}

// Main search function
async function searchLogs() {
    const query = document.getElementById('searchInput').value;
    const level = document.getElementById('levelFilter').value;
    const source = document.getElementById('sourceFilter').value;
    
    currentSearchParams = {
        query: query,
        level: level || "",
        sourceApp: source || "",
        page: 0,
        size: pageSize
    };
    
    currentPage = 0;
    hasMoreLogs = true;
    await searchLogsInternal(currentSearchParams);
}

// Internal search function
async function searchLogsInternal(params) {
    try {
        console.log('🔍 Searching with params:', params);
        
        const response = await fetch(`${API_BASE}/query/search`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(params)
        });
        
        const data = await response.json();
        console.log('📊 Search response:', data);
        
        if (data.success) {
            const logs = data.logs || [];
            console.log(`📈 Found ${logs.length} logs, total: ${data.totalElements}`);
            
            if (params.page === 0) {
                // First page - replace all logs
                allLogs = logs;
                displayLogs(allLogs);
            } else {
                // Subsequent page - append logs
                allLogs = [...allLogs, ...logs];
                displayLogs(allLogs);
            }
            
            // Check if there are more logs
            hasMoreLogs = (params.page + 1) < data.totalPages;
            updateLoadMoreButton();
            
            updateLastUpdateTime();
        } else {
            console.error('Search failed:', data.message);
            showError('Search failed: ' + data.message);
        }
    } catch (error) {
        console.error('❌ Error searching logs:', error);
        showError('Failed to load logs. Make sure services are running.');
    }
}

// Load more logs
async function loadMoreLogs() {
    if (!hasMoreLogs) {
        console.log('No more logs to load');
        return;
    }
    
    currentPage++;
    const params = {
        ...currentSearchParams,
        page: currentPage,
        size: pageSize
    };
    
    await searchLogsInternal(params);
}

// Update Load More button state
function updateLoadMoreButton() {
    const button = document.querySelector('button[onclick="loadMoreLogs()"]');
    if (button) {
        if (!hasMoreLogs) {
            button.disabled = true;
            button.innerHTML = '<i class="fas fa-check"></i> All logs loaded';
            button.className = 'btn btn-success';
        } else {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-plus"></i> Load More';
            button.className = 'btn btn-outline-primary';
        }
    }
}

// Display logs in table
function displayLogs(logs) {
    const tableBody = document.getElementById('logsTableBody');
    
    if (currentPage === 0) {
        tableBody.innerHTML = ''; // Clear for first page
    }
    
    if (logs.length === 0 && currentPage === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted py-4">
                    <i class="fas fa-inbox fa-2x mb-2"></i>
                    <p>No logs found</p>
                    <small class="text-muted">Try adjusting your filters</small>
                </td>
            </tr>
        `;
        return;
    }
    
    logs.forEach((log, index) => {
        // Skip if already displayed (for pagination)
        if (currentPage > 0 && index < currentPage * pageSize) {
            return;
        }
        
        const row = document.createElement('tr');
        row.className = `log-card log-${log.level.toLowerCase()}`;
        
        const time = new Date(log.timestamp).toLocaleTimeString();
        const levelClass = getLevelClass(log.level);
        
        row.innerHTML = `
            <td>${time}</td>
            <td><span class="badge ${levelClass}">${log.level}</span></td>
            <td><span class="badge bg-secondary">${log.sourceApp}</span></td>
            <td>
                <div class="log-message">${log.message}</div>
                ${displayMetadata(log.metadata)}
            </td>
            <td><small class="text-muted">${log.traceId || 'N/A'}</small></td>
            <td>
                <button class="btn btn-sm btn-outline-info" onclick="viewLogDetails('${log.logId}')">
                    <i class="fas fa-eye"></i>
                </button>
            </td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Get CSS class for log level
function getLevelClass(level) {
    switch(level) {
        case 'INFO': return 'bg-info';
        case 'WARN': return 'bg-warning';
        case 'ERROR': return 'bg-danger';
        case 'DEBUG': return 'bg-secondary';
        default: return 'bg-light text-dark';
    }
}

// Display metadata as badges
function displayMetadata(metadata) {
    if (!metadata || Object.keys(metadata).length === 0) return '';
    
    let badges = '';
    for (const [key, value] of Object.entries(metadata)) {
        badges += `<span class="badge metadata-badge bg-dark me-1">${key}: ${value}</span>`;
    }
    return `<div class="mt-1">${badges}</div>`;
}

// View log details
async function viewLogDetails(logId) {
    try {
        const response = await fetch(`${API_BASE}/query/log/${logId}`);
        const data = await response.json();
        
        if (data.success) {
            document.getElementById('logDetails').textContent = 
                JSON.stringify(data.log, null, 2);
            
            const modal = new bootstrap.Modal(document.getElementById('logModal'));
            modal.show();
        }
    } catch (error) {
        console.error('Error loading log details:', error);
        showError('Failed to load log details');
    }
}

// Clear search
function clearSearch() {
    document.getElementById('searchInput').value = '';
    document.getElementById('levelFilter').value = '';
    document.getElementById('sourceFilter').value = '';
    loadAllLogs();
}

// Refresh stats
function refreshStats() {
    loadStats();
    showToast('Stats refreshed');
}

// Load available sources
async function loadSources() {
    try {
        // Get unique sources from recent logs
        const response = await fetch(`${API_BASE}/query/search`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({query: "", page: 0, size: 100})
        });
        
        const data = await response.json();
        if (data.success) {
            const sources = new Set();
            data.logs.forEach(log => {
                if (log.sourceApp) sources.add(log.sourceApp);
            });
            
            const select = document.getElementById('sourceFilter');
            select.innerHTML = '<option value="">All Sources</option>';
            
            sources.forEach(source => {
                const option = document.createElement('option');
                option.value = source;
                option.textContent = source;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Error loading sources:', error);
    }
}

// Update last update time
function updateLastUpdateTime() {
    const now = new Date();
    const timeStr = now.toLocaleTimeString();
    document.getElementById('lastUpdate').textContent = `Last update: ${timeStr}`;
}

// Error handling
function showError(message) {
    showToast(message, 'danger');
}

// Success toast
function showToast(message, type = 'info') {
    // Create toast element
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-bg-${type} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;
    
    // Add to container
    const container = document.getElementById('toastContainer') || createToastContainer();
    container.innerHTML += toastHtml;
    
    // Show toast
    const toastEl = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastEl, {delay: 3000});
    toast.show();
    
    // Remove after hide
    toastEl.addEventListener('hidden.bs.toast', () => {
        toastEl.remove();
    });
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
    document.body.appendChild(container);
    return container;
}

// Auto-refresh stats every 30 seconds
setInterval(refreshStats, 30000);