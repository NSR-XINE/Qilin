let currentTheme = 'dark';

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
}
