let sessionId = null;

const messagesEl = document.getElementById('messages');
const questionEl = document.getElementById('question');
const sendBtn = document.getElementById('sendBtn');
const docFileEl = document.getElementById('docFile');
const docListEl = document.getElementById('docList');
const sessionInfoEl = document.getElementById('sessionInfo');
const sessionListEl = document.getElementById('sessionList');

/* ---------- 工具 ---------- */
function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/** 轻量 markdown 渲染：**加粗**、`代码`、换行 */
function renderMarkdown(text) {
  let html = escapeHtml(text);
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
  html = html.replace(/\n/g, '<br>');
  return html;
}

function scrollBottom() {
  messagesEl.scrollTop = messagesEl.scrollHeight;
}

function fmtTime(t) {
  if (!t) return '';
  const d = new Date(t);
  const p = n => String(n).padStart(2, '0');
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

function truncate(s, n) {
  return s && s.length > n ? s.slice(0, n) + '…' : (s || '');
}

/* ---------- 会话 ---------- */
function newSession() {
  sessionId = null;
  messagesEl.innerHTML = '';
  sessionInfoEl.textContent = '未创建会话（发送第一条消息后自动创建）';
  loadSessions();
  loadDocs();
}

async function openSession(sid) {
  sessionId = sid;
  sessionInfoEl.textContent = '会话 ID: ' + sid;
  messagesEl.innerHTML = '';
  const loading = appendLoading();
  try {
    const res = await fetch('/api/chat/history/' + encodeURIComponent(sid));
    const data = await res.json();
    if (data.code === 200) {
      (data.data || []).forEach(rec => {
        appendUser(rec.question);
        appendAssistant(rec.answer, rec.citations);
      });
    } else {
      appendAssistant('加载历史失败：' + data.message);
    }
  } catch (e) {
    appendAssistant('加载历史失败：' + e.message);
  } finally {
    loading.remove();
    loadSessions();
  }
}

async function loadSessions() {
  try {
    const res = await fetch('/api/chat/sessions');
    const data = await res.json();
    sessionListEl.innerHTML = '';
    if (data.code === 200 && data.data.length) {
      data.data.forEach(s => {
        const li = document.createElement('li');
        if (s.sessionId === sessionId) li.className = 'active-session';
        li.style.cursor = 'pointer';

        const left = document.createElement('div');
        const q = document.createElement('div');
        q.textContent = '📝 ' + truncate(s.lastQuestion, 18);
        const meta = document.createElement('div');
        meta.className = 'meta';
        meta.textContent = `${s.messageCount} 轮 · ${fmtTime(s.lastTime)}`;
        left.appendChild(q);
        left.appendChild(meta);

        const del = document.createElement('button');
        del.className = 'del';
        del.textContent = '✕';
        del.title = '删除会话';
        del.onclick = (e) => {
          e.stopPropagation();
          deleteSession(s.sessionId);
        };

        li.appendChild(left);
        li.appendChild(del);
        li.onclick = () => openSession(s.sessionId);
        sessionListEl.appendChild(li);
      });
    } else {
      sessionListEl.innerHTML = '<li style="font-size:12px;color:#94a3b8;border:none">暂无历史会话</li>';
    }
  } catch (e) {
    sessionListEl.innerHTML = '<li style="color:#ef4444;border:none">加载会话失败</li>';
  }
}

async function deleteSession(sid) {
  if (!confirm('确认删除该会话的全部对话记录？')) return;
  try {
    const res = await fetch('/api/chat/sessions/' + encodeURIComponent(sid), { method: 'DELETE' });
    if (res.ok) {
      if (sid === sessionId) {
        newSession();
      } else {
        loadSessions();
      }
    } else {
      alert('删除失败');
    }
  } catch (e) {
    alert('删除失败：' + e.message);
  }
}

/* ---------- 消息渲染 ---------- */
function appendUser(text) {
  const div = document.createElement('div');
  div.className = 'msg user';
  div.textContent = text;
  messagesEl.appendChild(div);
  scrollBottom();
}

function appendAssistant(text, citations) {
  const div = document.createElement('div');
  div.className = 'msg assistant';
  const content = document.createElement('div');
  content.innerHTML = renderMarkdown(text);
  div.appendChild(content);

  (citations || []).forEach(c => {
    const det = document.createElement('details');
    det.className = 'citation';
    const sum = document.createElement('summary');
    const score = c.score != null ? c.score.toFixed(3) : '-';
    sum.textContent = `📎 来源《${c.docName}》 · 相似度 ${score}`;
    const txt = document.createElement('div');
    txt.className = 'citation-text';
    txt.textContent = c.text;
    det.appendChild(sum);
    det.appendChild(txt);
    div.appendChild(det);
  });

  messagesEl.appendChild(div);
  scrollBottom();
}

function appendLoading() {
  const div = document.createElement('div');
  div.className = 'msg assistant typing';
  div.textContent = '思考中';
  messagesEl.appendChild(div);
  scrollBottom();
  return div;
}

/* ---------- 发送消息 ---------- */
async function sendMessage() {
  const q = questionEl.value.trim();
  if (!q) return;
  appendUser(q);
  questionEl.value = '';
  sendBtn.disabled = true;
  const loading = appendLoading();
  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, question: q })
    });
    const data = await res.json();
    if (data.code === 200) {
      sessionId = data.data.sessionId;
      sessionInfoEl.textContent = '会话 ID: ' + sessionId;
      appendAssistant(data.data.answer, data.data.citations);
      loadSessions();
    } else {
      appendAssistant('出错了：' + data.message);
    }
  } catch (e) {
    appendAssistant('请求失败：' + e.message);
  } finally {
    loading.remove();
    sendBtn.disabled = false;
  }
}

/* ---------- 文档管理 ---------- */
async function loadDocs() {
  try {
    const res = await fetch('/api/documents');
    const data = await res.json();
    docListEl.innerHTML = '';
    if (data.code === 200 && data.data.length) {
      data.data.forEach(doc => {
        const li = document.createElement('li');
        const left = document.createElement('div');
        const name = document.createElement('div');
        name.textContent = doc.name;
        const meta = document.createElement('div');
        meta.className = 'meta';
        const date = doc.createdAt ? doc.createdAt.slice(0, 10) : '';
        meta.textContent = `${doc.fileType} · ${doc.chunkCount} 块 · ${date}`;
        left.appendChild(name);
        left.appendChild(meta);

        const del = document.createElement('button');
        del.className = 'del';
        del.textContent = '✕';
        del.title = '删除文档';
        del.onclick = () => deleteDoc(doc.id);

        li.appendChild(left);
        li.appendChild(del);
        docListEl.appendChild(li);
      });
    } else {
      docListEl.innerHTML = '<li style="font-size:12px;color:#94a3b8;border:none">暂无文档，先上传一个吧</li>';
    }
  } catch (e) {
    docListEl.innerHTML = '<li style="color:#ef4444;border:none">加载文档失败</li>';
  }
}

async function deleteDoc(id) {
  if (!confirm('确认删除该文档及其向量数据？')) return;
  try {
    await fetch('/api/documents/' + id, { method: 'DELETE' });
    loadDocs();
  } catch (e) {
    alert('删除失败');
  }
}

docFileEl.addEventListener('change', async () => {
  const file = docFileEl.files[0];
  if (!file) return;
  const fd = new FormData();
  fd.append('file', file);
  appendAssistant(`开始上传文档：${file.name} …`);
  const loading = appendLoading();
  try {
    const res = await fetch('/api/documents/upload', { method: 'POST', body: fd });
    const data = await res.json();
    loading.remove();
    if (data.code === 200) {
      appendAssistant(`✅ 文档「${data.data.name}」上传成功，已切分 ${data.data.chunkCount} 块并入库`);
      loadDocs();
    } else {
      appendAssistant('❌ 上传失败：' + data.message);
    }
  } catch (e) {
    loading.remove();
    appendAssistant('❌ 上传请求失败：' + e.message);
  }
  docFileEl.value = '';
});

/* ---------- 事件绑定 ---------- */
sendBtn.addEventListener('click', sendMessage);
questionEl.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
});
document.getElementById('newSessionBtn').addEventListener('click', newSession);

/* 初始化 */
newSession();
