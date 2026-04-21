(function () {
    var listEl = document.getElementById('adminConversationList');
    var messagesEl = document.getElementById('adminThreadMessages');
    var titleEl = document.getElementById('adminThreadTitle');
    var form = document.getElementById('adminChatForm');
    var input = document.getElementById('adminChatInput');
    var sendBtn = document.getElementById('adminChatSendBtn');

    if (!listEl || !messagesEl || !titleEl || !form || !input || !sendBtn) {
        return;
    }

    var currentUsername = null;

    function formatTime(value) {
        if (!value) {
            return '';
        }
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return '';
        }
        return date.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' });
    }

    function renderConversations(items) {
        listEl.innerHTML = '';
        if (!items || items.length === 0) {
            listEl.innerHTML = '<p class="admin-chat-empty">Chưa có hội thoại nào.</p>';
            return;
        }

        items.forEach(function (item) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'admin-chat-conversation-item' + (item.username === currentUsername ? ' active' : '');
            btn.innerHTML = '<strong>' + item.username + '</strong><small>' + (item.lastMessage || '') + '</small>';
            btn.addEventListener('click', function () {
                currentUsername = item.username;
                input.disabled = false;
                sendBtn.disabled = false;
                titleEl.textContent = 'Đang chat với: ' + currentUsername;
                loadConversations();
                loadMessages();
            });
            listEl.appendChild(btn);
        });
    }

    function renderMessages(messages) {
        messagesEl.innerHTML = '';
        messages.forEach(function (msg) {
            var item = document.createElement('div');
            var isAdmin = msg.senderRole === 'ADMIN';
            item.className = 'admin-chat-message ' + (isAdmin ? 'admin-chat-message-admin' : 'admin-chat-message-user');
            item.innerHTML = '<p>' + (msg.content || '') + '</p><small>'
                + (isAdmin ? 'Admin' : msg.username) + ' • ' + formatTime(msg.createdAt) + '</small>';
            messagesEl.appendChild(item);
        });
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function loadConversations() {
        fetch('/chat/api/admin/conversations', { credentials: 'same-origin' })
            .then(function (res) { return res.json(); })
            .then(renderConversations)
            .catch(function () {
            });
    }

    function loadMessages() {
        if (!currentUsername) {
            return;
        }
        fetch('/chat/api/admin/messages/' + encodeURIComponent(currentUsername), { credentials: 'same-origin' })
            .then(function (res) { return res.json(); })
            .then(renderMessages)
            .catch(function () {
            });
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (!currentUsername) {
            return;
        }

        var content = input.value.trim();
        if (!content) {
            return;
        }

        fetch('/chat/api/admin/messages/' + encodeURIComponent(currentUsername), {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: content })
        })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('send failed');
                }
                input.value = '';
                loadMessages();
                loadConversations();
            })
            .catch(function () {
            });
    });

    loadConversations();
    setInterval(function () {
        loadConversations();
        loadMessages();
    }, 4000);
})();
