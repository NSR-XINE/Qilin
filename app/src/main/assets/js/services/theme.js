let currentTheme = 'dark';
let currentAccent = '';
let dynamicColorsActive = false;

const ACCENT_PRESETS = {
    'cyan':     { name: 'Cyan',     light: '#009aa6', dark: '#00f2fe' },
    'purple':   { name: 'Purple',   light: '#7b2cbf', dark: '#9d4edd' },
    'rose':     { name: 'Rose',     light: '#d90429', dark: '#ff2a5f' },
    'green':    { name: 'Green',    light: '#2ea043', dark: '#3fb950' },
    'orange':   { name: 'Orange',   light: '#d97706', dark: '#f0883e' },
    'blue':     { name: 'Blue',     light: '#2563eb', dark: '#58a6ff' },
    'teal':     { name: 'Teal',     light: '#0d9488', dark: '#2dd4bf' },
    'pink':     { name: 'Pink',     light: '#be185d', dark: '#f472b6' },
};

function getAccentPresetValue(presetKey) {
    const preset = ACCENT_PRESETS[presetKey];
    if (!preset) return '';
    return currentTheme === 'light' ? preset.light : preset.dark;
}

function hexToRgb(hex) {
    const v = parseInt(hex.replace('#', ''), 16);
    return [(v >> 16) & 255, (v >> 8) & 255, v & 255];
}

function applyAccentColor(colorHex) {
    if (!colorHex || colorHex === 'default') {
        document.body.removeAttribute('data-accent');
        return;
    }
    const rgb = hexToRgb(colorHex);
    const r = rgb[0], g = rgb[1], b = rgb[2];
    const lighter = `rgba(${r},${g},${b},0.15)`;
    document.body.setAttribute('data-accent', colorHex);
    document.body.style.setProperty('--accent-override', colorHex);
    document.body.style.setProperty('--accent-override-rgb', `${r},${g},${b}`);
}

async function tryApplyDynamicColors() {
    if (DB._available && typeof AndroidApp.getDynamicColors === 'function') {
        try {
            const raw = AndroidApp.getDynamicColors();
            if (raw && raw !== 'null') {
                const colors = JSON.parse(raw);
                applyAccentColor(colors.primary);
                dynamicColorsActive = true;
                return true;
            }
        } catch (e) {
            console.error('Dynamic colors failed', e);
        }
    }
    return false;
}

function getSavedAccent() {
    let accent = '';
    if (DB._available) accent = DB.getSetting('pref_accent_color', '');
    if (!accent) accent = DB.getLocal('pref_accent_color') || '';
    return accent;
}

function saveAccentColor(value) {
    DB.saveSetting('pref_accent_color', value);
    DB.setLocal('pref_accent_color', value);
}

function setAccentColor(value) {
    currentAccent = value;
    dynamicColorsActive = false;
    if (!value || value === 'default' || value === 'dynamic') {
        if (value === 'dynamic') {
            tryApplyDynamicColors().then(ok => {
                if (!ok) applyAccentColor('');
            });
        } else {
            applyAccentColor('');
        }
    } else {
        applyAccentColor(value);
    }
    saveAccentColor(currentAccent);
    updateAccentUI();
}

function updateAccentUI() {
    document.querySelectorAll('.accent-btn').forEach(btn => {
        const val = btn.dataset.accent || '';
        btn.classList.toggle('active', val === currentAccent || (val === 'default' && !currentAccent));
    });
}

function setThemeMode(mode) {
    currentTheme = mode;
    document.body.classList.remove('light-theme', 'amoled');
    if (mode === 'light') document.body.classList.add('light-theme');
    else if (mode === 'amoled') document.body.classList.add('amoled');

    DB.saveSetting('app_theme', mode);
    DB.setLocal('app_theme', mode);

    document.querySelectorAll('.theme-btn').forEach(b => b.classList.remove('active'));
    const activeBtn = document.getElementById(`btn-theme-${mode}`);
    if (activeBtn) activeBtn.classList.add('active');

    if (DB._available) DB.setSystemThemeMode(mode);
    updateThemeMeta();

    if (currentAccent === 'dynamic') {
        tryApplyDynamicColors();
    } else if (currentAccent && currentAccent !== 'default') {
        applyAccentColor(currentAccent);
    } else {
        applyAccentColor('');
    }
}

function updateThemeMeta() {
    const meta = document.querySelector('meta[name="theme-color"]');
    if (!meta) {
        const m = document.createElement('meta');
        m.name = 'theme-color';
        document.head.appendChild(m);
    }
    const el = document.querySelector('meta[name="theme-color"]');
    if (currentTheme === 'light') el.content = '#f0f2f5';
    else if (currentTheme === 'amoled') el.content = '#000000';
    else el.content = '#05060a';
}

function initTheme() {
    let savedTheme = 'dark';
    if (DB._available) {
        savedTheme = DB.getSetting('app_theme', '') || DB.getLocal('app_theme') || 'dark';
    } else {
        savedTheme = DB.getLocal('app_theme') || 'dark';
    }
    setThemeMode(savedTheme);

    const savedAccent = getSavedAccent();
    if (savedAccent) {
        currentAccent = savedAccent;
        if (savedAccent === 'dynamic') {
            tryApplyDynamicColors();
        } else if (savedAccent !== 'default') {
            applyAccentColor(savedAccent);
        }
    }
}
