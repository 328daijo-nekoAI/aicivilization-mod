package com.aicivilization.mod.dashboard;

public final class DashboardHtml {

    private DashboardHtml() {
    }

    public static final String PAGE = """
            <!DOCTYPE html>
            <html lang="ja">
            <head>
            <meta charset="UTF-8">
            <title>AI文明 管理ダッシュボード</title>
            <style>
                body { font-family: sans-serif; background: #1e1e24; color: #eee; margin: 0; padding: 20px; }
                h1 { color: #ffd479; }
                h2 { color: #9fd7ff; border-bottom: 1px solid #444; padding-bottom: 4px; }
                .panel { background: #2a2a33; border-radius: 8px; padding: 16px; margin-bottom: 20px; }
                table { width: 100%; border-collapse: collapse; }
                th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid #3a3a44; }
                input, select, textarea { background: #1e1e24; color: #eee; border: 1px solid #555; border-radius: 4px; padding: 6px; }
                button { background: #4a7dff; color: white; border: none; border-radius: 4px; padding: 6px 14px; cursor: pointer; }
                button.danger { background: #c04040; }
                button:hover { opacity: 0.85; }
                .assigned { color: #7fff7f; }
                .unassigned { color: #999; }
                textarea { width: 100%; height: 200px; font-family: monospace; }
                .log-entry { padding: 4px 0; border-bottom: 1px solid #333; font-size: 0.9em; }
                .tag { display: inline-block; background: #3a3a55; border-radius: 4px; padding: 1px 6px; margin-right: 6px; font-size: 0.8em; }
            </style>
            </head>
            <body>
            <h1>AI文明 管理ダッシュボード</h1>

            <div class="panel">
                <h2>脳（Groq APIプロファイル）</h2>
                <div>
                    <input id="newName" placeholder="プロファイル名">
                    <input id="newKey" placeholder="Groq APIキー" style="width:260px;">
                    <input id="newModel" placeholder="モデル名" value="openai/gpt-oss-120b" style="width:200px;">
                    <button onclick="addBrain()">追加</button>
                </div>
                <table id="brainsTable">
                    <thead><tr><th>名前</th><th>キー</th><th>モデル</th><th>状態</th><th></th></tr></thead>
                    <tbody></tbody>
                </table>
            </div>

            <div class="panel">
                <h2>AI市民の一覧と持ち物</h2>
                <div id="citizensList"></div>
            </div>

            <div class="panel">
                <h2>個体別記憶の閲覧・編集</h2>
                <div>
                    <select id="memorySelect" onchange="loadMemory()"></select>
                </div>
                <textarea id="memoryEditor" placeholder="脳を選択すると記憶データが表示されます"></textarea>
                <br><br>
                <button onclick="saveMemory()">保存</button>
            </div>

            <div class="panel">
                <h2>文明全体ログ（直近100件）</h2>
                <div id="logList"></div>
            </div>

            <script>
            async function fetchBrains() {
                const res = await fetch('/api/brains');
                return await res.json();
            }

            async function refreshBrains() {
                const brains = await fetchBrains();
                const tbody = document.querySelector('#brainsTable tbody');
                tbody.innerHTML = '';
                const select = document.getElementById('memorySelect');
                select.innerHTML = '<option value="">-- 脳を選択 --</option>';

                for (const b of brains) {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${escapeHtml(b.name)}</td>
                        <td>${escapeHtml(b.maskedKey)}</td>
                        <td>${escapeHtml(b.model)}</td>
                        <td class="${b.assigned ? 'assigned' : 'unassigned'}">${b.assigned ? '割当済' : '未割当'}</td>
                        <td><button class="danger" onclick="removeBrain('${b.id}')">削除</button></td>
                    `;
                    tbody.appendChild(tr);

                    const opt = document.createElement('option');
                    opt.value = b.id;
                    opt.textContent = b.name;
                    select.appendChild(opt);
                }
            }

            async function addBrain() {
                const name = document.getElementById('newName').value || '無題の脳';
                const apiKey = document.getElementById('newKey').value;
                const model = document.getElementById('newModel').value || 'openai/gpt-oss-120b';
                if (!apiKey) { alert('APIキーを入力してください'); return; }

                await fetch('/api/brains', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({name, apiKey, model})
                });
                document.getElementById('newName').value = '';
                document.getElementById('newKey').value = '';
                await refreshBrains();
            }

            async function removeBrain(id) {
                if (!confirm('この脳を削除しますか？')) return;
                await fetch('/api/brains/' + id, {method: 'DELETE'});
                await refreshBrains();
            }

            async function loadMemory() {
                const id = document.getElementById('memorySelect').value;
                if (!id) { document.getElementById('memoryEditor').value = ''; return; }
                const res = await fetch('/api/memory/' + id);
                if (!res.ok) { document.getElementById('memoryEditor').value = '(この脳にはまだ記憶データがありません)'; return; }
                const data = await res.json();
                document.getElementById('memoryEditor').value = JSON.stringify(data, null, 2);
            }

            async function saveMemory() {
                const id = document.getElementById('memorySelect').value;
                if (!id) { alert('脳を選択してください'); return; }
                const text = document.getElementById('memoryEditor').value;
                try {
                    JSON.parse(text);
                } catch (e) {
                    alert('JSONの形式が正しくありません: ' + e.message);
                    return;
                }
                await fetch('/api/memory/' + id, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: text
                });
                alert('保存しました');
            }

            async function refreshLog() {
                const res = await fetch('/api/log');
                const entries = await res.json();
                const list = document.getElementById('logList');
                list.innerHTML = '';
                for (const e of entries.slice().reverse()) {
                    const div = document.createElement('div');
                    div.className = 'log-entry';
                    const date = new Date(e.timestamp * 1000).toLocaleString();
                    div.innerHTML = `<span class="tag">${escapeHtml(e.category)}</span>${escapeHtml(e.message)} <span style="color:#777">(${date})</span>`;
                    list.appendChild(div);
                }
            }

            function escapeHtml(str) {
                const div = document.createElement('div');
                div.textContent = str ?? '';
                return div.innerHTML;
            }

            async function refreshCitizens() {
                const res = await fetch('/api/citizens');
                const citizens = await res.json();
                const list = document.getElementById('citizensList');
                list.innerHTML = '';

                if (citizens.length === 0) {
                    list.innerHTML = '<div style="color:#777">まだAIが出現していません。</div>';
                    return;
                }

                for (const c of citizens) {
                    const div = document.createElement('div');
                    div.style.padding = '8px 0';
                    div.style.borderBottom = '1px solid #3a3a44';

                    const itemsText = c.items.length === 0
                        ? '<span style="color:#777">（持ち物なし）</span>'
                        : c.items.map(i => `<span class="tag">${escapeHtml(i.item.replace('minecraft:', ''))} x${i.count}</span>`).join(' ');

                    div.innerHTML = `
                        <strong>${escapeHtml(c.name)}</strong>
                        ${c.isChild ? '<span class="tag">子供</span>' : ''}
                        <span class="tag">幸福度 ${Math.round(c.happiness)}</span>
                        <span class="tag">${c.hasHome ? '家あり' : '家なし'}</span>
                        <div style="margin-top:4px;">${itemsText}</div>
                    `;
                    list.appendChild(div);
                }
            }

            refreshBrains();
            refreshLog();
            refreshCitizens();
            setInterval(refreshLog, 10000);
            setInterval(refreshBrains, 15000);
            setInterval(refreshCitizens, 8000);
            </script>
            </body>
            </html>
            """;
}
