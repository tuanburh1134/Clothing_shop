(function () {
    var widget = document.getElementById('userChatWidget');
    var launcher = document.getElementById('chatLauncher');
    var panel = document.getElementById('chatPanel');
    var minimizeBtn = document.getElementById('chatMinimizeBtn');
    var expandBtn = document.getElementById('chatExpandBtn');
    var messagesEl = document.getElementById('chatMessages');
    var form = document.getElementById('chatForm');
    var input = document.getElementById('chatInput');

    if (!widget || !launcher || !panel || !messagesEl || !form || !input) {
        return;
    }

    var isExpanded = false;

    function scrollToBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function formatTime(value) {
        if (!value) {
            return '';
        }
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return '';
        }
        return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    }

    function renderMessages(messages) {
        messagesEl.innerHTML = '';
        messages.forEach(function (msg) {
            var item = document.createElement('div');
            var isUser = msg.senderRole === 'USER';
            item.className = 'chat-message ' + (isUser ? 'chat-message-user' : 'chat-message-admin');

            var text = document.createElement('p');
            text.textContent = msg.content || '';
            item.appendChild(text);

            var meta = document.createElement('small');
            meta.textContent = (isUser ? 'Bạn' : 'Admin') + ' • ' + formatTime(msg.createdAt);
            item.appendChild(meta);

            messagesEl.appendChild(item);
        });
        scrollToBottom();
    }

    function hideWidgetForAnonymous() {
        widget.style.display = 'none';
    }

    function loadMessages() {
        fetch('/chat/api/user/messages', { credentials: 'same-origin' })
            .then(function (res) {
                if (res.status === 401 || res.status === 403) {
                    hideWidgetForAnonymous();
                    throw new Error('unauthorized');
                }
                return res.json();
            })
            .then(renderMessages)
            .catch(function () {
            });
    }

    launcher.addEventListener('click', function () {
        panel.classList.remove('chat-panel-minimized');
        launcher.style.display = 'none';
        loadMessages();
    });

    minimizeBtn.addEventListener('click', function () {
        panel.classList.add('chat-panel-minimized');
        launcher.style.display = 'inline-flex';
    });

    expandBtn.addEventListener('click', function () {
        isExpanded = !isExpanded;
        panel.classList.toggle('chat-panel-expanded', isExpanded);
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var content = input.value.trim();
        if (!content) {
            return;
        }

        fetch('/chat/api/user/messages', {
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
                return loadMessages();
            })
            .catch(function () {
            });
    });

    setInterval(function () {
        if (!panel.classList.contains('chat-panel-minimized')) {
            loadMessages();
        }
    }, 4000);
})();
