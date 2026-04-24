// Configuration
const API_BASE = 'http://localhost:8080';

// Wizard state
let currentStep = 1;
let inferenceResult = null;
let sampleLogParsed = null;
let schemaSaved = false;

// Initialize
document.addEventListener('DOMContentLoaded', function () {
    loadSchemas();
});

// ── Schema List ──

async function loadSchemas() {
    try {
        const response = await fetch(`${API_BASE}/ingest/schemas`);
        const data = await response.json();

        if (data.success) {
            renderSchemaList(data.schemas || []);
        }
    } catch (error) {
        console.error('Error loading schemas:', error);
        document.getElementById('schemaList').innerHTML = `
            <div class="empty-state">
                <i class="fas fa-exclamation-triangle"></i>
                <p>Could not load schemas</p>
                <small>Make sure the services are running</small>
            </div>`;
    }
}

function renderSchemaList(schemas) {
    const container = document.getElementById('schemaList');

    if (schemas.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-layer-group"></i>
                <p>No schemas defined yet</p>
                <small>Create a schema to start ingesting custom log formats</small>
            </div>`;
        return;
    }

    container.innerHTML = schemas.map(schema => {
        const mappings = schema.fieldMappings || {};
        const customFields = schema.customFields || [];
        const searchableCount = customFields.filter(f => f.searchable).length;
        const created = schema.createdAt ? new Date(schema.createdAt).toLocaleDateString() : '';

        // Build field badges
        let badges = '';
        if (mappings.messageField) badges += `<span class="field-badge mapped">${mappings.messageField} → message</span>`;
        if (mappings.levelField) badges += `<span class="field-badge mapped">${mappings.levelField} → level</span>`;
        if (mappings.timestampField) badges += `<span class="field-badge mapped">${mappings.timestampField} → timestamp</span>`;
        if (searchableCount > 0) badges += `<span class="field-badge searchable">${searchableCount} searchable</span>`;
        customFields.filter(f => !f.searchable).forEach(f => {
            badges += `<span class="field-badge custom">${f.originalName}</span>`;
        });

        return `
            <div class="schema-row">
                <div style="flex:1;">
                    <div class="schema-name">${schema.name}</div>
                    ${schema.description ? `<div class="schema-desc">${schema.description}</div>` : ''}
                    <div style="margin-top:6px;">${badges}</div>
                    <div class="schema-meta" style="margin-top:4px;">
                        Source: <strong>${mappings.sourceAppValue || '—'}</strong> · Created: ${created}
                    </div>
                </div>
                <div class="d-flex gap-2 align-items-center">
                    <button class="btn-ghost" style="padding:6px 10px; font-size:0.8rem;" onclick="showPipeCommand('${schema.name}')">
                        <i class="fas fa-terminal"></i> Pipe
                    </button>
                    <button class="btn-danger-ghost" onclick="deleteSchema('${schema.id}')">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>`;
    }).join('');
}

function showPipeCommand(schemaName) {
    const cmd = `node app.js 2>&1 | node log-pipe.js --schema ${schemaName} --url http://localhost:8080`;
    const toastHtml = `
        <div style="background:#1e1e2e; color:#cdd6f4; border-radius:8px; padding:14px 18px; font-family:'SF Mono','Fira Code',monospace; font-size:0.82rem; max-width:500px;">
            <div style="margin-bottom:6px; color:var(--text-tertiary); font-family:inherit; font-size:0.72rem;">PIPE COMMAND</div>
            ${cmd}
        </div>`;
    showToast(toastHtml, 'dark');
}

async function deleteSchema(id) {
    if (!confirm('Delete this schema? Existing logs are not affected.')) return;

    try {
        const response = await fetch(`${API_BASE}/ingest/schemas/${id}`, { method: 'DELETE' });
        const data = await response.json();
        if (data.success) {
            showToast('Schema deleted', 'info');
            loadSchemas();
        } else {
            showToast('Failed to delete: ' + data.message, 'danger');
        }
    } catch (error) {
        showToast('Error deleting schema', 'danger');
    }
}

// ── Wizard ──

function openWizard() {
    currentStep = 1;
    inferenceResult = null;
    sampleLogParsed = null;
    schemaSaved = false;

    document.getElementById('sampleLogInput').value = '';
    document.getElementById('schemaName').value = '';
    document.getElementById('schemaDescription').value = '';
    document.getElementById('inferError').style.display = 'none';
    document.getElementById('saveError').style.display = 'none';
    document.getElementById('saveSuccess').style.display = 'none';

    updateWizardUI();
    const modal = new bootstrap.Modal(document.getElementById('wizardModal'));
    modal.show();
}

function updateWizardUI() {
    // Show/hide steps
    for (let i = 1; i <= 4; i++) {
        document.getElementById('step' + i).classList.toggle('active', i === currentStep);
        const dot = document.getElementById('stepDot' + i);
        dot.classList.remove('active', 'done');
        if (i === currentStep) dot.classList.add('active');
        else if (i < currentStep) dot.classList.add('done');
    }

    // Prev button
    document.getElementById('wizardPrev').style.display = currentStep > 1 && !schemaSaved ? '' : 'none';

    // Next button text
    const nextBtn = document.getElementById('wizardNext');
    if (schemaSaved) {
        nextBtn.textContent = 'Done';
        nextBtn.onclick = closeWizard;
    } else {
        switch (currentStep) {
            case 1:
                nextBtn.innerHTML = 'Analyze <i class="fas fa-arrow-right"></i>';
                nextBtn.onclick = wizardNext;
                break;
            case 2:
                nextBtn.innerHTML = 'Next <i class="fas fa-arrow-right"></i>';
                nextBtn.onclick = wizardNext;
                break;
            case 3:
                nextBtn.innerHTML = 'Next <i class="fas fa-arrow-right"></i>';
                nextBtn.onclick = wizardNext;
                break;
            case 4:
                nextBtn.innerHTML = '<i class="fas fa-save"></i> Save Schema';
                nextBtn.onclick = wizardNext;
                break;
        }
    }
}

async function wizardNext() {
    if (currentStep === 1) {
        // Analyze sample log
        const sampleLog = document.getElementById('sampleLogInput').value.trim();
        if (!sampleLog) {
            showInferError('Please paste a sample log');
            return;
        }

        // Try local parse first
        try {
            sampleLogParsed = JSON.parse(sampleLog);
        } catch (e) {
            showInferError('Invalid JSON. Please paste a valid JSON log line.');
            return;
        }

        try {
            const response = await fetch(`${API_BASE}/ingest/schemas/infer`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sampleLog: sampleLog })
            });
            const data = await response.json();

            if (data.success) {
                inferenceResult = data.result;
                populateMappingDropdowns();
                currentStep = 2;
            } else {
                showInferError(data.message || 'Failed to analyze log');
                return;
            }
        } catch (error) {
            showInferError('Could not reach the server. Are services running?');
            return;
        }
    } else if (currentStep === 2) {
        // Validate message mapping
        const messageField = document.getElementById('mapMessage').value;
        if (!messageField) {
            showToast('Message field is required', 'danger');
            return;
        }
        populateCustomFields();
        currentStep = 3;
    } else if (currentStep === 3) {
        currentStep = 4;
    } else if (currentStep === 4) {
        await saveSchema();
        return;
    }

    updateWizardUI();
}

function wizardPrev() {
    if (currentStep > 1) {
        currentStep--;
        updateWizardUI();
    }
}

function closeWizard() {
    bootstrap.Modal.getInstance(document.getElementById('wizardModal')).hide();
    loadSchemas();
}

function showInferError(msg) {
    const el = document.getElementById('inferError');
    el.textContent = msg;
    el.style.display = 'block';
}

// ── Step 2: Populate Mapping Dropdowns ──

function populateMappingDropdowns() {
    const fields = inferenceResult.detectedFields || [];
    const suggested = inferenceResult.suggestedMappings || {};

    const dropdowns = {
        mapMessage: suggested.messageField || '',
        mapLevel: suggested.levelField || '',
        mapTimestamp: suggested.timestampField || '',
        mapTraceId: suggested.traceIdField || ''
    };

    for (const [selectId, defaultVal] of Object.entries(dropdowns)) {
        const select = document.getElementById(selectId);
        select.innerHTML = '<option value="">(none)</option>';
        fields.forEach(f => {
            const option = document.createElement('option');
            option.value = f.name;
            option.textContent = `${f.name} (${f.type})`;
            if (f.name === defaultVal) option.selected = true;
            select.appendChild(option);
        });
    }
}

// ── Step 3: Populate Custom Fields ──

function populateCustomFields() {
    const fields = inferenceResult.detectedFields || [];

    // Get mapped fields to exclude
    const mapped = new Set([
        document.getElementById('mapMessage').value,
        document.getElementById('mapLevel').value,
        document.getElementById('mapTimestamp').value,
        document.getElementById('mapTraceId').value
    ].filter(v => v));

    const customFields = fields.filter(f => !mapped.has(f.name));
    const tbody = document.getElementById('customFieldsBody');
    const noCustom = document.getElementById('noCustomFields');

    if (customFields.length === 0) {
        tbody.innerHTML = '';
        noCustom.style.display = 'block';
        return;
    }

    noCustom.style.display = 'none';
    tbody.innerHTML = customFields.map(f => {
        const sampleVal = sampleLogParsed ? sampleLogParsed[f.name] : '';
        const displayVal = typeof sampleVal === 'object' ? JSON.stringify(sampleVal) : String(sampleVal || '');
        return `
            <tr>
                <td><strong>${f.name}</strong></td>
                <td><span class="field-badge custom">${f.type}</span></td>
                <td style="font-family:'SF Mono','Fira Code',monospace; font-size:0.78rem; color:var(--text-secondary); max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${displayVal}</td>
                <td style="text-align:center;">
                    <input type="checkbox" class="form-check-input" data-field="${f.name}" checked>
                </td>
            </tr>`;
    }).join('');
}

// ── Step 4: Save Schema ──

async function saveSchema() {
    const name = document.getElementById('schemaName').value.trim();
    if (!name) {
        showSaveError('Schema name is required');
        return;
    }

    // Validate name format (URL-friendly)
    if (!/^[a-zA-Z0-9_-]+$/.test(name)) {
        showSaveError('Name must only contain letters, numbers, hyphens, and underscores');
        return;
    }

    const description = document.getElementById('schemaDescription').value.trim();

    // Build field mappings
    const fieldMappings = {
        messageField: document.getElementById('mapMessage').value || '',
        levelField: document.getElementById('mapLevel').value || '',
        timestampField: document.getElementById('mapTimestamp').value || '',
        traceIdField: document.getElementById('mapTraceId').value || '',
        sourceAppValue: document.getElementById('mapSourceApp').value.trim() || name
    };

    // Build custom fields
    const mapped = new Set([
        fieldMappings.messageField, fieldMappings.levelField,
        fieldMappings.timestampField, fieldMappings.traceIdField
    ].filter(v => v));

    const customFields = [];
    const checkboxes = document.querySelectorAll('#customFieldsBody input[type="checkbox"]');
    const allFields = inferenceResult.detectedFields || [];

    allFields.filter(f => !mapped.has(f.name)).forEach(f => {
        const checkbox = document.querySelector(`#customFieldsBody input[data-field="${f.name}"]`);
        customFields.push({
            originalName: f.name,
            type: f.type,
            searchable: checkbox ? checkbox.checked : false
        });
    });

    const schema = {
        name,
        description: description || null,
        fieldMappings,
        customFields,
        sampleLog: document.getElementById('sampleLogInput').value.trim()
    };

    try {
        const response = await fetch(`${API_BASE}/ingest/schemas`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(schema)
        });
        const data = await response.json();

        if (data.success) {
            schemaSaved = true;
            document.getElementById('saveError').style.display = 'none';
            document.getElementById('saveSuccess').style.display = 'block';
            document.getElementById('pipeCommandText').textContent =
                `node app.js 2>&1 | node log-pipe.js --schema ${name} --url http://localhost:8080`;

            // Update step dots
            document.getElementById('stepDot4').classList.remove('active');
            document.getElementById('stepDot4').classList.add('done');

            updateWizardUI();
        } else {
            showSaveError(data.message || 'Failed to save schema');
        }
    } catch (error) {
        showSaveError('Could not reach the server');
    }
}

function showSaveError(msg) {
    const el = document.getElementById('saveError');
    el.textContent = msg;
    el.style.display = 'block';
}

function copyPipeCommand() {
    const text = document.getElementById('pipeCommandText').textContent;
    navigator.clipboard.writeText(text).then(() => {
        showToast('Copied to clipboard', 'info');
    });
}

// ── Toast ──

function showToast(message, type = 'info') {
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-bg-${type} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>`;

    const container = document.getElementById('toastContainer') || createToastContainer();
    container.innerHTML += toastHtml;

    const toastEl = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();

    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
    document.body.appendChild(container);
    return container;
}
