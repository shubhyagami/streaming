// Poweramp Web UI - Main Application Script

// ===== SPA Navigation =====
function showSection(name) {
    document.querySelectorAll('.page-section').forEach(s => s.classList.remove('active'));
    const section = document.getElementById('section-' + name);
    if (section) section.classList.add('active');
    document.querySelectorAll('.nav-btn').forEach(b => {
        b.classList.toggle('active', b.dataset.section === name);
    });
}

// ===== EQ 32-Band Frequencies =====
const EQ_FREQS = [
    20, 25, 31.5, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800,
    1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000, 20000
];

let currentGains = new Array(31).fill(0);
let currentPresetId = null;
let currentPresetName = 'Flat';
let autosaveEnabled = false;

// ===== Web Audio API Equalizer =====
let audioCtx = null;
let sourceNode = null;
let filterNodes = [];
let eqEnabled = true;

function initWebAudio() {
    const audio = document.getElementById('audio-player');
    if (!audio) return;
    if (!audioCtx) {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (audioCtx.state === 'suspended') {
        audioCtx.resume();
    }
    if (sourceNode) {
        try { sourceNode.disconnect(); } catch(e) {}
    }
    sourceNode = audioCtx.createMediaElementSource(audio);
    filterNodes = [];
    let prev = sourceNode;
    EQ_FREQS.forEach((freq, i) => {
        const filter = audioCtx.createBiquadFilter();
        filter.type = 'peaking';
        filter.frequency.value = freq;
        filter.Q.value = 0.707;
        filter.gain.value = currentGains[i] || 0;
        filterNodes.push(filter);
        prev.connect(filter);
        prev = filter;
    });
    if (filterNodes.length) {
        filterNodes[filterNodes.length - 1].connect(audioCtx.destination);
    }
}

function updateWebEq() {
    if (!audioCtx || filterNodes.length === 0) return;
    const enabled = document.getElementById('eq-enabled').checked && eqEnabled;
    currentGains.forEach((gain, i) => {
        if (filterNodes[i]) {
            filterNodes[i].gain.value = enabled ? gain : 0;
        }
    });
}

function toggleEqProcessing() {
    const eqOn = document.getElementById('eq-enabled').checked;
    const toneOn = document.getElementById('tone-enabled').checked;
    updateWebEq();
    updateToneWeb();
}

// Bass/Tone via Web Audio
let bassFilter = null;
let trebleFilter = null;

function initToneWeb() {
    if (!audioCtx) return;
    if (bassFilter || trebleFilter) return;
    bassFilter = audioCtx.createBiquadFilter();
    bassFilter.type = 'lowshelf';
    bassFilter.frequency.value = 250;
    bassFilter.gain.value = 0;

    trebleFilter = audioCtx.createBiquadFilter();
    trebleFilter.type = 'highshelf';
    trebleFilter.frequency.value = 4000;
    trebleFilter.gain.value = 0;
}

function updateToneWeb() {
    if (!bassFilter || !trebleFilter) return;
    const toneOn = document.getElementById('tone-enabled').checked;
    const bassVal = parseFloat(document.getElementById('bass-slider').value) || 0;
    const trebleVal = parseFloat(document.getElementById('treble-slider').value) || 0;
    bassFilter.gain.value = toneOn ? bassVal : 0;
    trebleFilter.gain.value = toneOn ? trebleVal : 0;
}

function reconnectWebAudio() {
    const audio = document.getElementById('audio-player');
    if (!audio || !audioCtx) return;
    if (sourceNode) {
        try { sourceNode.disconnect(); } catch(e) {}
    }
    sourceNode = audioCtx.createMediaElementSource(audio);
    // Reconnect analyserNode if it exists
    if (analyserNode) {
        try { analyserNode.disconnect(); } catch(e) {}
        sourceNode.connect(analyserNode);
    }
    if (filterNodes.length === 0) {
        initWebAudio();
        return;
    }
    let prev = sourceNode;
    filterNodes.forEach(f => {
        prev.connect(f);
        prev = f;
    });
    if (bassFilter && trebleFilter) {
        prev.connect(bassFilter);
        bassFilter.connect(trebleFilter);
        trebleFilter.connect(audioCtx.destination);
    } else if (filterNodes.length) {
        filterNodes[filterNodes.length - 1].connect(audioCtx.destination);
    }
}

// ===== LED Spectrum Analyzer =====
let analyserNode = null;
let spectrumAnimId = null;
let specColorHues = [];
let specColorTimer = 0;

function initSpectrumAnalyzer() {
    if (!audioCtx) return;
    if (analyserNode) {
        try { analyserNode.disconnect(); } catch(e) {}
    }
    analyserNode = audioCtx.createAnalyser();
    analyserNode.fftSize = 256;
    // Connect analyser in parallel from source (before filters = raw audio)
    if (sourceNode) {
        sourceNode.connect(analyserNode);
    }
    // Initialize random hues for each bar
    const numBars = analyserNode.frequencyBinCount;
    specColorHues = [];
    for (let i = 0; i < numBars; i++) {
        specColorHues.push(Math.random() * 360);
    }
    specColorTimer = 0;
    startSpectrum();
}

function startSpectrum() {
    if (spectrumAnimId) cancelAnimationFrame(spectrumAnimId);
    drawSpectrum();
}

function stopSpectrum() {
    if (spectrumAnimId) { cancelAnimationFrame(spectrumAnimId); spectrumAnimId = null; }
    const canvas = document.getElementById('spectrum-canvas');
    if (canvas) {
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
}

function drawSpectrum() {
    const canvas = document.getElementById('spectrum-canvas');
    if (!canvas || !analyserNode) { spectrumAnimId = requestAnimationFrame(drawSpectrum); return; }
    const ctx = canvas.getContext('2d');
    const w = canvas.width, h = canvas.height;
    const bufferLength = analyserNode.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);
    analyserNode.getByteFrequencyData(dataArray);

    ctx.clearRect(0, 0, w, h);

    // Randomize colors every ~30 frames
    specColorTimer++;
    if (specColorTimer > 30) {
        specColorTimer = 0;
        for (let i = 0; i < bufferLength; i++) {
            specColorHues[i] = (specColorHues[i] + (Math.random() * 60 - 30)) % 360;
            if (specColorHues[i] < 0) specColorHues[i] += 360;
        }
        // Update label
        const label = document.getElementById('spec-color-label');
        if (label) {
            const names = ['RETRO WAVE', 'SUNSET BOULEVARD', 'NEON NIGHTS', 'CYBERPUNK', 'VAPORWAVE', 'ARCADE FIRE', 'LASER DISCO', 'CHROME DOME', 'GLOW STICK', 'PIXEL RAIN'];
            label.textContent = names[Math.floor(Math.random() * names.length)];
        }
    }

    const barWidth = (w / bufferLength) * 1.4;
    const barGap = 1;
    const maxBarHeight = h - 4;

    for (let i = 0; i < bufferLength; i++) {
        const barHeight = (dataArray[i] / 255) * maxBarHeight;
        const x = i * (barWidth + barGap);
        const y = h - barHeight - 2;

        // LED-style bar with gradient glow
        const hue = specColorHues[i];
        const saturation = 90 + Math.random() * 10;
        const lightness = 50 + Math.random() * 20;

        const grad = ctx.createLinearGradient(x, y, x, h - 2);
        const topColor = `hsl(${hue}, ${saturation}%, ${lightness}%)`;
        const bottomColor = `hsl(${hue}, ${saturation}%, ${Math.max(10, lightness - 30)}%)`;
        grad.addColorStop(0, topColor);
        grad.addColorStop(0.6, bottomColor);
        grad.addColorStop(1, `hsl(${hue}, 40%, 5%)`);

        // Rounded top LED look
        const radius = 2;
        ctx.fillStyle = grad;
        ctx.shadowColor = `hsl(${hue}, 100%, 60%)`;
        ctx.shadowBlur = 6;
        ctx.beginPath();
        ctx.moveTo(x + radius, y);
        ctx.lineTo(x + barWidth - radius, y);
        ctx.quadraticCurveTo(x + barWidth, y, x + barWidth, y + radius);
        ctx.lineTo(x + barWidth, h - 2);
        ctx.lineTo(x, h - 2);
        ctx.lineTo(x, y + radius);
        ctx.quadraticCurveTo(x, y, x + radius, y);
        ctx.closePath();
        ctx.fill();
        ctx.shadowBlur = 0;
    }

    spectrumAnimId = requestAnimationFrame(drawSpectrum);
}

// ===== Initialize Equalizer =====
function initEqualizer() {
    renderSliders();
    loadPresetList();
    updateFrs();
}

function renderSliders() {
    const container = document.getElementById('eq-sliders');
    if (!container) return;
    container.innerHTML = '';
    EQ_FREQS.forEach((freq, i) => {
        const col = document.createElement('div');
        col.className = 'eq-slider-col';
        const freqLabel = document.createElement('div');
        freqLabel.className = 'freq-label';
        freqLabel.textContent = formatFreq(freq);
        col.appendChild(freqLabel);
        const slider = document.createElement('input');
        slider.type = 'range';
        slider.min = -12; slider.max = 12; slider.step = 0.5; slider.value = 0;
        slider.dataset.index = i;
        slider.addEventListener('input', function() {
            const idx = parseInt(this.dataset.index);
            currentGains[idx] = parseFloat(this.value);
            const gainLabel = this.closest('.eq-slider-col').querySelector('.gain-label');
            const val = this.value;
            gainLabel.textContent = (val > 0 ? '+' : '') + val + ' dB';
            updateFrs();
            updateWebEq();
        });
        col.appendChild(slider);
        const gainLabel = document.createElement('div');
        gainLabel.className = 'gain-label';
        gainLabel.textContent = '0 dB';
        col.appendChild(gainLabel);
        container.appendChild(col);
    });
}

function formatFreq(freq) {
    if (freq >= 1000) return (freq / 1000) + 'k';
    return freq.toString();
}

// ===== Frequency Response Graph =====
function updateFrs() {
    const canvas = document.getElementById('frs-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;
    ctx.clearRect(0, 0, w, h);
    const minFreq = 10, maxFreq = 20000, minDb = -24, maxDb = 24;
    const points = [];
    for (let i = 0; i < 200; i++) {
        const freq = minFreq * Math.pow(maxFreq / minFreq, i / 199);
        let mag = 0;
        currentGains.forEach((gain, bandIdx) => {
            if (Math.abs(gain) > 0.1) {
                const f0 = EQ_FREQS[bandIdx];
                if (f0 <= 0) return;
                const omega = 2 * Math.PI * freq / 44100;
                const sn = Math.sin(omega), cs = Math.cos(omega);
                const A = Math.pow(10, gain / 40), alpha = sn / (2 * 0.707);
                const b0 = 1 + alpha * A, b1 = -2 * cs, b2 = 1 - alpha * A;
                const a0 = 1 + alpha / A, a1 = -2 * cs, a2 = 1 - alpha / A;
                const b0_2 = b0 + b1 * cs + b2 * Math.cos(2 * omega);
                const b0_i = b1 * sn + b2 * Math.sin(2 * omega);
                const a0_2 = a0 + a1 * cs + a2 * Math.cos(2 * omega);
                const a0_i = a1 * sn + a2 * Math.sin(2 * omega);
                const num = b0_2 * b0_2 + b0_i * b0_i;
                const den = a0_2 * a0_2 + a0_i * a0_i;
                if (den > 0) mag += 10 * Math.log10(num / den);
            }
        });
        points.push({ freq, mag });
    }
    ctx.strokeStyle = '#444444'; ctx.lineWidth = 0.5;
    for (let db = -24; db <= 24; db += 6) {
        const y = h - ((db - minDb) / (maxDb - minDb)) * h;
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
    }
    const x0 = 0, y0 = h - ((0 - minDb) / (maxDb - minDb)) * h;
    ctx.beginPath(); ctx.moveTo(x0, y0);
    points.forEach((p, i) => {
        const x = (i / 199) * w, y = h - ((p.mag - minDb) / (maxDb - minDb)) * h;
        ctx.lineTo(x, y);
    });
    ctx.lineTo(w, y0); ctx.closePath();
    ctx.fillStyle = 'rgba(255,255,255,0.12)'; ctx.fill();
    ctx.beginPath();
    points.forEach((p, i) => {
        const x = (i / 199) * w, y = h - ((p.mag - minDb) / (maxDb - minDb)) * h;
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    });
    ctx.strokeStyle = '#ffffff'; ctx.lineWidth = 2; ctx.stroke();
    ctx.beginPath(); ctx.moveTo(0, y0); ctx.lineTo(w, y0);
    ctx.strokeStyle = '#777777'; ctx.lineWidth = 1; ctx.setLineDash([4, 4]); ctx.stroke(); ctx.setLineDash([]);
}

// ===== EQ Tab Switching =====
function switchEqTab(tab) {
    document.querySelectorAll('.eq-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.eq-tab-panel').forEach(p => p.classList.remove('active'));
    document.querySelector(`.eq-tab[data-tab="${tab}"]`).classList.add('active');
    document.getElementById(`${tab}-tab-content`).classList.add('active');
}

// ===== Tone Controls =====
function updateTone() {
    const bass = document.getElementById('bass-slider');
    const treble = document.getElementById('treble-slider');
    if (bass) document.getElementById('bass-value').textContent = bass.value + ' dB';
    if (treble) document.getElementById('treble-value').textContent = treble.value + ' dB';
    updateFrs();
    updateToneWeb();
}

// ===== Volume Controls =====
function updateVolume() {
    const bal = document.getElementById('balance-slider');
    if (bal) document.getElementById('balance-value').textContent = bal.value;
    const sfx = document.getElementById('sfx-slider');
    if (sfx) document.getElementById('sfx-value').textContent = Math.round(sfx.value * 100) + '%';
    const tempo = document.getElementById('tempo-slider');
    if (tempo) document.getElementById('tempo-value').textContent = parseFloat(tempo.value).toFixed(2) + 'x';
    const vol = document.getElementById('volume-slider');
    if (vol) {
        document.getElementById('volume-value').textContent = Math.round(vol.value * 100) + '%';
        const audio = document.getElementById('audio-player');
        if (audio) audio.volume = parseFloat(vol.value);
    }
}

function resetVolume() {
    document.getElementById('balance-slider').value = 0.5;
    document.getElementById('sfx-slider').value = 0;
    document.getElementById('tempo-slider').value = 1;
    document.getElementById('tempo-enabled').checked = false;
    document.getElementById('volume-slider').value = 1;
    document.getElementById('mono-enabled').checked = false;
    updateVolume();
}

// ===== Reverb Controls =====
function updateReverb() {
    const map = { 'rev-damp': 'rev-damp-val', 'rev-filter': 'rev-filter-val',
        'rev-fade': 'rev-fade-val', 'rev-predelay': 'rev-predelay-val',
        'rev-size': 'rev-size-val', 'rev-mix': 'rev-mix-val' };
    Object.entries(map).forEach(([id, valId]) => {
        const el = document.getElementById(id), valEl = document.getElementById(valId);
        if (el && valEl) valEl.textContent = id === 'rev-predelay' ? el.value + 'ms' : el.value + '%';
    });
}

function loadReverbPreset(name) {
    const presets = {
        auditorium: { damp: 60, filter: 40, fade: 60, predelay: 30, size: 70, mix: 35 },
        hall: { damp: 50, filter: 50, fade: 50, predelay: 25, size: 60, mix: 30 },
        room: { damp: 40, filter: 60, fade: 30, predelay: 10, size: 30, mix: 25 },
        echo: { damp: 20, filter: 30, fade: 80, predelay: 80, size: 40, mix: 40 },
        studio: { damp: 70, filter: 70, fade: 20, predelay: 5, size: 20, mix: 20 }
    };
    const p = presets[name];
    if (!p) return;
    Object.keys(p).forEach(k => {
        const el = document.getElementById('rev-' + (k === 'predelay' ? 'predelay' : k));
        if (el) el.value = p[k];
    });
    updateReverb();
}

// ===== EQ Menu =====
function toggleEqMenu() { document.getElementById('eq-menu-popup').classList.toggle('hidden'); }
function toggleAutoSave() { autosaveEnabled = !autosaveEnabled; }

function resetEq() {
    currentGains.fill(0);
    document.querySelectorAll('.eq-slider-col input[type=range]').forEach(s => s.value = 0);
    document.querySelectorAll('.gain-label').forEach(l => l.textContent = '0 dB');
    document.getElementById('bass-slider').value = 0;
    document.getElementById('treble-slider').value = 0;
    document.getElementById('eq-enabled').checked = true;
    document.getElementById('tone-enabled').checked = false;
    document.getElementById('limiter-enabled').checked = true;
    updateFrs();
    updateWebEq();
    updateToneWeb();
}

// ===== Preset Management =====
function loadPresetList() {
    const presetSelect = document.getElementById('preset-select');
    if (!presetSelect) return;
    fetch('/api/presets').then(r => r.json()).then(presets => {
        presetSelect.innerHTML = '<option value="">Select preset...</option>';
        const cats = { BUILT_IN: [], USER: [], AUTO_EQ: [] };
        presets.forEach(p => { if (cats[p.category]) cats[p.category].push(p); });
        ['BUILT_IN', 'USER'].forEach(cat => {
            if (cats[cat].length) {
                const opt = document.createElement('option');
                opt.disabled = true; opt.textContent = '--- ' + cat.replace('_', ' ') + ' ---';
                presetSelect.appendChild(opt);
                cats[cat].forEach(p => addPresetOption(presetSelect, p));
            }
        });
    });
}

function addPresetOption(select, preset) {
    const opt = document.createElement('option');
    opt.value = preset.id; opt.textContent = preset.name;
    select.appendChild(opt);
}

function loadPreset(presetId) {
    if (!presetId) return;
    fetch('/api/presets/' + presetId).then(r => r.json()).then(preset => {
        currentPresetId = preset.id; currentPresetName = preset.name;
        if (preset.mode === 'GRAPHIC' && preset.bands.length === 31) {
            const sliders = document.querySelectorAll('.eq-slider-col input[type=range]');
            preset.bands.forEach((band, i) => {
                if (sliders[i]) {
                    sliders[i].value = band.gain;
                    currentGains[i] = band.gain;
                    const gl = sliders[i].closest('.eq-slider-col').querySelector('.gain-label');
                    gl.textContent = (band.gain > 0 ? '+' : '') + band.gain + ' dB';
                }
            });
            updateFrs();
            updateWebEq();
        }
    });
}

function savePreset() {
    const name = prompt('Preset name:', currentPresetName + ' (modified)');
    if (!name) return;
    const bands = currentGains.map((gain, i) => ({
        frequency: EQ_FREQS[i], gain, q: 0.707, type: 'PEAKING', channels: 3, locked: false, color: 0
    }));
    fetch('/api/presets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, mode: 'GRAPHIC', preamp: 0, category: 'USER', bands, description: 'User preset from web UI' })
    }).then(r => r.json()).then(() => {
        loadPresetList();
        document.getElementById('eq-menu-popup').classList.add('hidden');
    });
}

function exportPresets() {
    fetch('/api/presets/export').then(r => r.text()).then(json => {
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url; a.download = 'poweramp-presets.json'; a.click();
        URL.revokeObjectURL(url);
    });
}

function importPresets(event) {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function(e) {
        fetch('/api/presets/import', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: e.target.result })
            .then(r => r.json()).then(() => { loadPresetList(); loadPresetListCards(); });
    };
    reader.readAsText(file);
}

// ===== Presets Page =====
function loadPresetListCards() {
    const list = document.getElementById('preset-list');
    if (!list) return;
    fetch('/api/presets').then(r => r.json()).then(presets => {
        list.innerHTML = '';
        presets.forEach(p => {
            const item = document.createElement('div');
            item.className = 'preset-item';
            item.innerHTML = `<div><div class="preset-name">${p.name}</div><div class="preset-meta">${p.mode} | ${p.bands.length} bands | ${p.description || ''}</div></div><span class="preset-cat">${p.category}</span>`;
            item.onclick = () => showPresetDetail(p);
            list.appendChild(item);
        });
    });
}

function showPresetDetail(preset) {
    const detail = document.getElementById('preset-detail');
    detail.classList.remove('hidden');
    document.getElementById('detail-name').textContent = preset.name + ' (' + preset.mode + ')';
    const bandsDiv = document.getElementById('detail-bands');
    bandsDiv.innerHTML = '<h3>Bands</h3>';
    const bt = document.createElement('div');
    bt.style.cssText = 'display:grid;grid-template-columns:repeat(4,1fr);gap:2px;font-size:11px;color:#aaa';
    bt.innerHTML = '<span>Freq</span><span>Gain</span><span>Q</span><span>Type</span>';
    preset.bands.forEach(b => { bt.innerHTML += `<span>${b.frequency}Hz</span><span>${b.gain}dB</span><span>${b.q}</span><span>${b.type}</span>`; });
    bandsDiv.appendChild(bt);
    detail.dataset.presetId = preset.id;
}

function applyPresetDetail() {
    const id = document.getElementById('preset-detail').dataset.presetId;
    if (id) showSection('equalizer');
}

function deletePresetDetail() {
    const id = document.getElementById('preset-detail').dataset.presetId;
    if (!id) return;
    fetch('/api/presets/' + id, { method: 'DELETE' }).then(() => {
        loadPresetListCards();
        document.getElementById('preset-detail').classList.add('hidden');
    });
}

function showNewPresetDialog() { document.getElementById('new-preset-dialog').classList.remove('hidden'); }
function closeDialog(id) { document.getElementById(id).classList.add('hidden'); }

function createPreset() {
    const name = document.getElementById('new-preset-name').value;
    const mode = document.getElementById('new-preset-mode').value;
    if (!name) return;
    const bands = EQ_FREQS.map(f => ({ frequency: f, gain: 0, q: 0.707, type: 'PEAKING', channels: 3, locked: false, color: 0 }));
    fetch('/api/presets', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name, mode, preamp: 0, bands, description: '' }) })
        .then(r => r.json()).then(() => { closeDialog('new-preset-dialog'); loadPresetListCards(); loadPresetList(); });
}

function filterPresets() {
    const search = (document.getElementById('preset-search').value || '').toLowerCase();
    const cat = document.getElementById('preset-category-filter').value;
    document.querySelectorAll('.preset-item').forEach(item => {
        const name = item.querySelector('.preset-name').textContent.toLowerCase();
        const category = item.querySelector('.preset-cat').textContent;
        item.style.display = (!search || name.includes(search)) && (cat === 'ALL' || category === cat) ? 'flex' : 'none';
    });
}

// ===== Dashboard =====
function loadQuickPresets() {
    const pills = document.getElementById('quick-presets');
    if (!pills) return;
    fetch('/api/presets').then(r => r.json()).then(presets => {
        pills.innerHTML = '';
        presets.slice(0, 12).forEach(p => {
            const pill = document.createElement('div');
            pill.className = 'preset-pill';
            pill.textContent = p.name;
            pill.onclick = () => { showSection('equalizer'); };
            pills.appendChild(pill);
        });
    });
}

document.addEventListener('click', function(e) {
    const menu = document.getElementById('eq-menu-popup');
    if (menu && !menu.classList.contains('hidden') && !e.target.closest('.eq-menu-btn') && !e.target.closest('.eq-menu-popup')) {
        menu.classList.add('hidden');
    }
    const dialog = document.getElementById('new-preset-dialog');
    if (dialog && !dialog.classList.contains('hidden') && e.target === dialog) {
        dialog.classList.add('hidden');
    }
});

// ===== Source Toggle (YouTube / Spotify) =====
let currentSource = 'youtube';
function switchSource(source) {
    currentSource = source;
    document.querySelectorAll('.source-option').forEach(el => el.classList.toggle('active', el.dataset.source === source));
    const input = document.getElementById('search-input');
    input.placeholder = source === 'youtube' ? 'Search YouTube Music...' : 'Search on Spotify...';
    document.getElementById('search-results').innerHTML = '';
    document.getElementById('search-status').innerHTML = '';
}

// ===== YouTube/Spotify Search & Playback =====
let currentToken = null;
let currentVideoId = null;
const PLAYER_STORAGE_KEY = 'powerampPlayer';

function savePlayerState() {
    const audio = document.getElementById('audio-player');
    if (!audio || !currentToken) return;
    try {
        sessionStorage.setItem(PLAYER_STORAGE_KEY, JSON.stringify({
            token: currentToken, videoId: currentVideoId, thumbnail: currentThumbnail,
            title: (document.getElementById('np-title') || {}).textContent || '',
            currentTime: audio.currentTime || 0, duration: audio.duration || 0, paused: audio.paused
        }));
    } catch(e) {}
}

function restorePlayerState() {
    try {
        const saved = sessionStorage.getItem(PLAYER_STORAGE_KEY);
        if (!saved) return;
        const state = JSON.parse(saved);
        if (!state.token) { sessionStorage.removeItem(PLAYER_STORAGE_KEY); return; }
        currentToken = state.token; currentVideoId = state.videoId;
        currentThumbnail = state.thumbnail || null;
        const np = document.getElementById('now-playing');
        np.classList.remove('hidden');
        document.getElementById('np-title').textContent = state.title;
        setStatus('Playing: ' + state.title);
        const audio = document.getElementById('audio-player');
        audio.src = '/api/yt/stream/' + state.token;
        audio.ontimeupdate = function() {
            if (audio.duration) {
                document.getElementById('np-current').textContent = formatTime(audio.currentTime);
                document.getElementById('np-duration').textContent = formatTime(audio.duration);
                updateCassette();
                savePlayerState();
            }
        };
        audio.onended = function() { stopPlayback(); };
        audio.onloadedmetadata = function() {
            if (state.currentTime > 0) audio.currentTime = Math.min(state.currentTime, audio.duration || 0);
            if (!state.paused) {
                audio.play();
                document.getElementById('play-pause-icon').innerHTML = '<path fill="currentColor" d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
                initWebAudio();
                initToneWeb();
                reconnectWebAudio();
            }
            startWave();
            updateCassette();
        };
        audio.onerror = function() {
            sessionStorage.removeItem(PLAYER_STORAGE_KEY);
            np.classList.add('hidden');
            setStatus('Session expired — search again');
            currentToken = null; currentVideoId = null;
        };
            audio.onplay = function() {
                if (!audioCtx) { initWebAudio(); initToneWeb(); reconnectWebAudio(); initSpectrumAnalyzer(); }
                else if (audioCtx.state === 'suspended') { audioCtx.resume(); initSpectrumAnalyzer(); }
            };
    } catch(e) { sessionStorage.removeItem(PLAYER_STORAGE_KEY); }
}

function clearPlayerState() { try { sessionStorage.removeItem(PLAYER_STORAGE_KEY); } catch(e) {} }

function doSearch() {
    if (currentSource === 'youtube') searchYouTube();
    else searchSpotify();
}

function searchYouTube() {
    const q = document.getElementById('search-input').value.trim();
    if (!q) return;
    document.getElementById('search-status').innerHTML = '<span class="status-msg">Searching YouTube...</span>';
    document.getElementById('search-results').innerHTML = '';
    fetch('/api/yt/search?q=' + encodeURIComponent(q))
        .then(r => { if (!r.ok) throw new Error('Search failed'); return r.json(); })
        .then(data => {
            document.getElementById('search-status').innerHTML = '';
            if (!data || data.length === 0) { document.getElementById('search-status').innerHTML = '<span class="status-msg">No results found</span>'; return; }
            renderYTResults(data);
        })
        .catch(err => { document.getElementById('search-status').innerHTML = '<span class="status-msg error">Error: ' + err.message + '</span>'; });
}

function searchSpotify() {
    const q = document.getElementById('search-input').value.trim();
    if (!q) return;
    document.getElementById('search-status').innerHTML = '<span class="status-msg">Searching Spotify...</span>';
    document.getElementById('search-results').innerHTML = '';
    fetch('/api/spotify/search?q=' + encodeURIComponent(q))
        .then(r => { if (!r.ok) throw new Error('Search failed'); return r.json(); })
        .then(data => {
            document.getElementById('search-status').innerHTML = '';
            if (!data || data.length === 0) { document.getElementById('search-status').innerHTML = '<span class="status-msg">No results found</span>'; return; }
            renderSpotifyResults(data);
        })
        .catch(err => { document.getElementById('search-status').innerHTML = '<span class="status-msg error">Error: ' + err.message + '</span>'; });
}

function renderYTResults(results) {
    const container = document.getElementById('search-results');
    container.innerHTML = '';
    results.forEach(r => {
        const item = document.createElement('div');
        item.className = 'yt-result';
        item.innerHTML = `
            <div class="yt-result-thumb"><img src="${r.thumbnail}" alt="" onerror="this.style.display='none'" style="width:60px;height:45px;object-fit:cover;border-radius:4px;background:var(--bg-card)"></div>
            <div class="yt-result-info">
                <div class="yt-result-title">${escapeHtml(r.title)}</div>
                <div class="yt-result-channel">${escapeHtml(r.channel)}</div>
                <div class="yt-result-duration">${formatDuration(r.duration)}</div>
            </div>
            <button class="yt-play-btn" onclick="playVideo('${r.videoId}', '${escapeHtml(r.title).replace(/'/g, "\\'")}', '${escapeHtml(r.thumbnail).replace(/'/g, "\\'")}')"><svg width="24" height="24" viewBox="0 0 24 24"><path fill="currentColor" d="M8 5v14l11-7z"/></svg></button>
        `;
        container.appendChild(item);
    });
}

function renderSpotifyResults(results) {
    const container = document.getElementById('search-results');
    container.innerHTML = '';
    results.forEach(r => {
        const item = document.createElement('div');
        item.className = 'yt-result';
        const hasAudioUrl = r.audioUrl && r.audioUrl.length > 0;
        const escapedTitle = escapeHtml(r.title).replace(/'/g, "\\'");
        const playAttr = hasAudioUrl
            ? `playSpotify('${escapeHtml(r.audioUrl).replace(/'/g, "\\'")}', '', '${escapedTitle}')`
            : `playSpotify('', '${r.videoId}', '${escapedTitle}')`;
        item.innerHTML = `
            <div class="yt-result-thumb"><div style="width:60px;height:45px;border-radius:4px;background:var(--bg-card);display:flex;align-items:center;justify-content:center;"><svg width="20" height="20" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/></svg></div></div>
            <div class="yt-result-info">
                <div class="yt-result-title">${escapeHtml(r.title)}</div>
                <div class="yt-result-channel">${escapeHtml(r.artist)}</div>
                <div class="yt-result-duration">${r.size || ''}</div>
            </div>
            <button class="yt-play-btn" onclick="${playAttr}"><svg width="24" height="24" viewBox="0 0 24 24"><path fill="currentColor" d="M8 5v14l11-7z"/></svg></button>
        `;
        container.appendChild(item);
    });
}

function playVideo(videoId, title, thumbnail) {
    currentThumbnail = thumbnail || null;
    const spinner = document.getElementById('loading-spinner');
    spinner.classList.remove('hidden');
    currentVideoId = videoId;
    fetch('/api/yt/stream?videoId=' + encodeURIComponent(videoId) + '&title=' + encodeURIComponent(title), { method: 'POST' })
        .then(async r => { if (!r.ok) { const errBody = await r.json().catch(() => ({})); throw new Error(errBody.error || 'Download failed'); } return r.json(); })
        .then(data => {
            spinner.classList.add('hidden');
            currentToken = data.token;
            const audio = document.getElementById('audio-player');
            audio.src = data.streamUrl;
            audio.play().then(() => {
                initWebAudio();
                initToneWeb();
                reconnectWebAudio();
                initSpectrumAnalyzer();
            }).catch(function(){});
            showNowPlaying(title, currentThumbnail);
            setStatus('Playing: ' + title);
            savePlayerState();
        })
        .catch(err => { spinner.classList.add('hidden'); document.getElementById('search-status').innerHTML = '<span class="status-msg error">' + err.message + '</span>'; });
}

function playSpotify(audioUrl, videoId, title) {
    const spinner = document.getElementById('loading-spinner');
    spinner.classList.remove('hidden');
    currentVideoId = videoId || 'spotify';
    const params = 'title=' + encodeURIComponent(title)
        + (audioUrl ? '&audioUrl=' + encodeURIComponent(audioUrl) : '')
        + (videoId ? '&videoId=' + encodeURIComponent(videoId) : '');
    fetch('/api/spotify/stream?' + params, { method: 'POST' })
        .then(async r => { if (!r.ok) { const errBody = await r.json().catch(() => ({})); throw new Error(errBody.error || 'Download failed'); } return r.json(); })
        .then(data => {
            spinner.classList.add('hidden');
            currentToken = data.token;
            const audio = document.getElementById('audio-player');
            audio.src = data.streamUrl;
            audio.play().then(() => {
                initWebAudio();
                initToneWeb();
                reconnectWebAudio();
                initSpectrumAnalyzer();
            }).catch(function(){});
            showNowPlaying(title, currentThumbnail);
            setStatus('Playing: ' + title);
            savePlayerState();
        })
        .catch(err => { spinner.classList.add('hidden'); document.getElementById('search-status').innerHTML = '<span class="status-msg error">' + err.message + '</span>'; });
}

// ===== Cassette Player =====
let currentThumbnail = null;

function updateCassette() {
    const wrapper = document.getElementById('cassette-wrapper');
    if (!wrapper) return;
    const audio = document.getElementById('audio-player');
    const isPlaying = audio && audio.src && !audio.paused && audio.duration;
    const reels = document.querySelectorAll('.reel-group');
    const titleEl = document.getElementById('cassette-title-svg');
    const imgEl = document.getElementById('cassette-img-svg');
    const currentEl = document.getElementById('cassette-current');
    const durEl = document.getElementById('cassette-duration');
    const fillEl = document.getElementById('cassette-progress-fill');

    if (isPlaying) {
        reels.forEach(r => r.classList.add('spinning'));
        const title = document.getElementById('np-title').textContent;
        if (titleEl) {
            while (titleEl.firstChild) titleEl.removeChild(titleEl.firstChild);
            titleEl.appendChild(document.createTextNode((title || 'NO TRACK').substring(0, 22)));
        }
        if (imgEl) {
            if (currentThumbnail) {
                imgEl.setAttributeNS('http://www.w3.org/1999/xlink', 'href', currentThumbnail);
                imgEl.setAttribute('opacity', '1');
            } else {
                imgEl.setAttribute('opacity', '0');
            }
        }
        if (currentEl) currentEl.textContent = formatTime(audio.currentTime);
        if (durEl) durEl.textContent = formatTime(audio.duration);
        if (fillEl) fillEl.style.width = ((audio.currentTime / audio.duration) * 100) + '%';
    } else {
        reels.forEach(r => r.classList.remove('spinning'));
        if (titleEl) {
            while (titleEl.firstChild) titleEl.removeChild(titleEl.firstChild);
            titleEl.appendChild(document.createTextNode('NO TRACK'));
        }
        if (imgEl) imgEl.setAttribute('opacity', '0');
        if (currentEl) currentEl.textContent = '0:00';
        if (durEl) durEl.textContent = '0:00';
        if (fillEl) fillEl.style.width = '0%';
    }
}

// ===== Sinewave Seekbar =====
let waveAnimId = null;
let waveTime = 0;

function seekTo(event) {
    const audio = document.getElementById('audio-player');
    if (!audio.duration) return;
    const bg = document.getElementById('seekbar-bg');
    const rect = bg.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const pct = Math.max(0, Math.min(1, x / rect.width));
    audio.currentTime = pct * audio.duration;
}

function initWaveCanvas() {
    const canvas = document.getElementById('wave-canvas');
    if (!canvas) return;
    const bg = document.getElementById('seekbar-bg');
    canvas.width = bg.offsetWidth;
    canvas.height = bg.offsetHeight;
}

function drawWave() {
    const canvas = document.getElementById('wave-canvas');
    const audio = document.getElementById('audio-player');
    if (!canvas || !audio) { waveAnimId = null; return; }

    const ctx = canvas.getContext('2d');
    const w = canvas.width, h = canvas.height;
    const progress = audio.duration ? audio.currentTime / audio.duration : 0;

    ctx.clearRect(0, 0, w, h);

    // Background fill up to progress
    const midY = h / 2;
    const amplitude = Math.min(10, h / 4);
    const freq = 0.04;
    waveTime += 0.03;

    // Draw filled sinewave up to progress point
    ctx.beginPath();
    ctx.moveTo(0, h);
    for (let x = 0; x <= w * progress; x++) {
        const y = midY + amplitude * Math.sin(freq * x + waveTime);
        ctx.lineTo(x, y);
    }
    ctx.lineTo(w * progress, h);
    ctx.closePath();
    ctx.fillStyle = 'rgba(255,255,255,0.15)';
    ctx.fill();

    // Draw sinewave line up to progress
    ctx.beginPath();
    for (let x = 0; x <= w * progress; x++) {
        const y = midY + amplitude * Math.sin(freq * x + waveTime);
        if (x === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
    }
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Draw dim sinewave for unplayed portion
    if (progress < 1) {
        ctx.beginPath();
        for (let x = w * progress; x <= w; x++) {
            const y = midY + amplitude * Math.sin(freq * x + waveTime);
            if (x === w * progress) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        }
        ctx.strokeStyle = 'rgba(255,255,255,0.15)';
        ctx.lineWidth = 1;
        ctx.stroke();
    }

    waveAnimId = requestAnimationFrame(drawWave);
}

function startWave() {
    initWaveCanvas();
    if (waveAnimId) cancelAnimationFrame(waveAnimId);
    drawWave();
}

function stopWave() {
    if (waveAnimId) { cancelAnimationFrame(waveAnimId); waveAnimId = null; }
    const canvas = document.getElementById('wave-canvas');
    if (canvas) {
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
}

if (typeof window !== 'undefined') {
    window.addEventListener('resize', function() {
        if (waveAnimId) initWaveCanvas();
    });
}

function showNowPlaying(title, thumbnail) {
    currentThumbnail = thumbnail || null;
    const np = document.getElementById('now-playing');
    np.classList.remove('hidden');
    document.getElementById('np-title').textContent = title;
    document.getElementById('np-current').textContent = '0:00';
    const audio = document.getElementById('audio-player');
    audio.ontimeupdate = function() {
        if (audio.duration) {
            document.getElementById('np-current').textContent = formatTime(audio.currentTime);
            document.getElementById('np-duration').textContent = formatTime(audio.duration);
            updateCassette();
            savePlayerState();
        }
    };
    audio.onended = function() { stopPlayback(); };
    startWave();
    updateCassette();
}

function togglePlayPause() {
    const audio = document.getElementById('audio-player');
    const icon = document.getElementById('play-pause-icon');
    if (audio.paused) {
        audio.play();
        icon.innerHTML = '<path fill="currentColor" d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
        startWave();
    } else {
        audio.pause();
        icon.innerHTML = '<path fill="currentColor" d="M8 5v14l11-7z"/>';
    }
    updateCassette();
}

function stopPlayback() {
    const audio = document.getElementById('audio-player');
    audio.pause();
    audio.src = '';
    document.getElementById('now-playing').classList.add('hidden');
    document.getElementById('play-pause-icon').innerHTML = '<path fill="currentColor" d="M8 5v14l11-7z"/>';
    if (currentToken) { fetch('/api/yt/stop/' + currentToken, { method: 'POST' }).catch(function(){}); currentToken = null; }
    currentVideoId = null;
    currentThumbnail = null;
    clearPlayerState();
    setStatus('Ready');
    if (sourceNode) { try { sourceNode.disconnect(); } catch(e) {} sourceNode = null; }
    stopWave();
    stopSpectrum();
    updateCassette();
}

function setStatus(msg) { const el = document.getElementById('status-text'); if (el) el.textContent = msg; }

function formatDuration(seconds) {
    if (!seconds || seconds <= 0) return '--:--';
    const m = Math.floor(seconds / 60), s = Math.floor(seconds % 60);
    return m + ':' + (s < 10 ? '0' : '') + s;
}

function formatTime(t) {
    if (!t || isNaN(t)) return '0:00';
    const m = Math.floor(t / 60), s = Math.floor(t % 60);
    return m + ':' + (s < 10 ? '0' : '') + s;
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str; return div.innerHTML;
}

// ===== Initialization =====
document.addEventListener('DOMContentLoaded', function() {
    const url = window.location.pathname;
    const sectionMap = { '/equalizer': 'equalizer', '/player': 'player', '/presets': 'presets', '/processing': 'processing' };
    const section = sectionMap[url] || 'dashboard';
    showSection(section);

    renderSliders();
    loadPresetList();
    updateFrs();
    loadQuickPresets();
    restorePlayerState();
});

window.addEventListener('beforeunload', function() { savePlayerState(); });
