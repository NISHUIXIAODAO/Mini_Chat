// 聊天功能的JavaScript代码
document.addEventListener('DOMContentLoaded', function() {
    // 检查用户是否已登录
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    const nickName = localStorage.getItem('nickName');
    
    if (!token || !userId) {
        // 未登录，重定向到登录页面
        window.location.href = 'index.html';
        return;
    }
    
    // 设置当前用户信息
    const currentUserAvatar = document.getElementById('current-user-avatar');
    const currentUserName = document.getElementById('current-user-name');
    
    currentUserAvatar.textContent = nickName ? nickName.charAt(0).toUpperCase() : 'U';
    currentUserName.textContent = nickName || '用户';
    
    // DOM元素
    const contactList = document.getElementById('contact-list');
    const chatTitle = document.getElementById('chat-title');
    const chatMessages = document.getElementById('chat-messages');
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-button');
    const addFriendBtn = document.getElementById('add-friend-btn');
    const addFriendModal = document.getElementById('add-friend-modal');
    const closeModal = document.querySelector('.close-modal');
    const submitFriendRequest = document.getElementById('submit-friend-request');
    
    // 当前选中的联系人
    let currentContactId = null;
    
    // 加载联系人列表
    function loadContacts() {
        fetch('/userContact/getContactList', {
            method: 'GET',
            headers: {
                'Authorization': token
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200 && data.data) {
                contactList.innerHTML = '';
                
                // 渲染联系人列表
                data.data.forEach(contact => {
                    const contactItem = document.createElement('div');
                    contactItem.className = 'contact-item';
                    contactItem.dataset.contactId = contact.contactId;
                    
                    const avatar = document.createElement('div');
                    avatar.className = 'contact-avatar';
                    avatar.textContent = contact.nickName.charAt(0).toUpperCase();
                    
                    const info = document.createElement('div');
                    info.className = 'contact-info';
                    
                    const name = document.createElement('div');
                    name.className = 'contact-name';
                    name.textContent = contact.nickName;
                    
                    const lastMessage = document.createElement('div');
                    lastMessage.className = 'contact-last-message';
                    lastMessage.textContent = contact.lastMessage || '暂无消息';
                    
                    info.appendChild(name);
                    info.appendChild(lastMessage);
                    
                    contactItem.appendChild(avatar);
                    contactItem.appendChild(info);
                    
                    // 点击联系人加载聊天记录
                    contactItem.addEventListener('click', function() {
                        // 移除其他联系人的active类
                        document.querySelectorAll('.contact-item').forEach(item => {
                            item.classList.remove('active');
                        });
                        
                        // 添加active类到当前联系人
                        contactItem.classList.add('active');
                        
                        // 设置当前联系人ID和标题
                        currentContactId = contact.contactId;
                        chatTitle.textContent = contact.nickName;
                        
                        // 启用输入框和发送按钮
                        messageInput.disabled = false;
                        sendButton.disabled = false;
                        
                        // 加载聊天记录
                        loadChatHistory(currentContactId);
                    });
                    
                    contactList.appendChild(contactItem);
                });
            } else {
                console.error('加载联系人失败:', data.msg);
            }
        })
        .catch(error => {
            console.error('请求联系人列表出错:', error);
        });
    }
    
    // 加载聊天记录
    function loadChatHistory(contactId) {
        fetch(`/chat/getMessageHistory?contactId=${contactId}`, {
            method: 'GET',
            headers: {
                'Authorization': token
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200 && data.data) {
                chatMessages.innerHTML = '';
                
                // 确保消息按时间顺序排序（如果后端未排序）
                const sortedMessages = [...data.data].sort((a, b) => {
                    // sendTime字段，按时间排序
                    if (a.sendTime && b.sendTime) {
                        return new Date(a.sendTime) - new Date(b.sendTime);
                    }
                    return 0; // 保持原顺序
                });
                
                // 渲染聊天记录
                sortedMessages.forEach(message => {
                    addMessageToChat(message);
                });
                
                // 滚动到最新消息
                chatMessages.scrollTop = chatMessages.scrollHeight;
            } else {
                console.error('加载聊天记录失败:', data.msg);
            }
        })
        .catch(error => {
            console.error('请求聊天记录出错:', error);
        });
    }
    
    // 添加消息到聊天区域
    function addMessageToChat(message) {
        // 检查消息是否有效
        if (!message || !message.messageContent) {
            console.error('无效的消息对象:', message);
            return;
        }
        
        const messageElement = document.createElement('div');
        messageElement.className = message.senderId == userId ? 'message sent' : 'message received';
        
        const messageContent = document.createElement('div');
        messageContent.className = 'message-content';
        messageContent.textContent = message.messageContent;
        
        // 添加时间戳属性用于消息去重
        if (message.createTime) {
            messageElement.dataset.timestamp = message.createTime;
        } else {
            messageElement.dataset.timestamp = Date.now();
        }
        
        messageElement.appendChild(messageContent);
        chatMessages.appendChild(messageElement);
        
        // 滚动到最新消息
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
    
    // 发送消息
    function sendMessage() {
        const content = messageInput.value.trim();
        
        if (!content || !currentContactId) {
            return;
        }
        
        const messageData = {
            contactId: currentContactId,
            messageContent: content,
            messageType: 2, // 文本消息
            sendUserId: userId
        };
        
        // 立即在前端显示发送的消息
        const newMessage = {
            senderId: userId,
            messageContent: content
        };
        addMessageToChat(newMessage);
        
        // 清空输入框
        messageInput.value = '';
        
        // 通过WebSocket发送消息
        const ws = getWebSocketInstance();
        const wsSuccess = ws.isConnected() && ws.sendMessage(messageData);
        
        // 同时通过HTTP接口发送（作为备份方式）
        fetch('/chat/sendMessage', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token
            },
            body: JSON.stringify(messageData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.code !== 200) {
                console.error('发送消息失败:', data.msg);
                alert('发送消息失败: ' + data.msg);
            }
        })
        .catch(error => {
            console.error('发送消息请求出错:', error);
            alert('发送消息请求出错，请稍后再试');
        });
    }
    
    // 发送按钮点击事件
    sendButton.addEventListener('click', sendMessage);
    
    // 输入框回车发送
    messageInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendMessage();
        }
    });
    
    // 添加好友按钮点击事件
    addFriendBtn.addEventListener('click', function() {
        addFriendModal.style.display = 'block';
    });
    
    // 关闭模态框
    closeModal.addEventListener('click', function() {
        addFriendModal.style.display = 'none';
    });
    
    // 点击模态框外部关闭
    window.addEventListener('click', function(event) {
        if (event.target == addFriendModal) {
            addFriendModal.style.display = 'none';
        }
    });
    
    // 提交好友申请
    submitFriendRequest.addEventListener('click', function() {
        const friendId = document.getElementById('friend-id').value;
        const applyMessage = document.getElementById('apply-message').value || '请求添加您为好友';
        
        if (!friendId) {
            alert('请输入好友ID');
            return;
        }
        
        fetch(`/userContact/applyFriendAdd?contactId=${friendId}&applyInfo=${encodeURIComponent(applyMessage)}`, {
            method: 'POST' ,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('好友申请已发送');
                addFriendModal.style.display = 'none';
            } else {
                alert('发送好友申请失败: ' + data.msg);
            }
        })
        .catch(error => {
            console.error('发送好友申请出错:', error);
            alert('发送好友申请出错，请稍后再试');
        });
    });
    
    // 初始加载联系人列表
    loadContacts();
    
    // 初始化WebSocket消息处理
    const ws = getWebSocketInstance();
    
    // 监听WebSocket聊天消息
    ws.onChatMessage(message => {
        // 检查消息是否有效
        if (!message || !message.messageContent) {
            console.error('收到无效的WebSocket消息:', message);
            return;
        }
        
        // 检查是否是当前正在聊天的联系人发来的消息
        if (currentContactId && (message.sendUserId == currentContactId || message.contactId == currentContactId)) {
            // 检查消息是否已经显示（防止重复显示）
            const existingMessages = chatMessages.querySelectorAll('.message');
            let isDuplicate = false;
            
            // 使用消息内容和发送者ID来判断是否为重复消息
            for (let i = 0; i < existingMessages.length; i++) {
                const msgContent = existingMessages[i].querySelector('.message-content').textContent;
                const isSent = existingMessages[i].classList.contains('sent');
                const msgSenderId = isSent ? userId : message.sendUserId;
                
                if (msgContent === message.messageContent && msgSenderId == message.sendUserId) {
                    // 如果消息内容和发送者都相同，认为是重复消息
                    isDuplicate = true;
                    break;
                }
            }
            
            // 如果不是重复消息，则添加到聊天区域
            if (!isDuplicate) {
                const messageObj = {
                    senderId: message.sendUserId,
                    messageContent: message.messageContent,
                    createTime: message.sendTime || Date.now()
                };
                addMessageToChat(messageObj);
            }
        } else {
            // 只有在不是当前聊天的联系人发来消息时才更新联系人列表
            loadContacts();
        }
    });
    
    // 监听系统通知和好友请求，同样需要更新联系人列表
    ws.onSystemNotice(() => loadContacts());
    ws.onFriendRequest(() => loadContacts());
});

// 添加CSS样式
document.head.insertAdjacentHTML('beforeend', `
<style>
.modal {
    display: none;
    position: fixed;
    z-index: 1000;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
}

.modal-content {
    background-color: white;
    margin: 15% auto;
    padding: 20px;
    border-radius: 10px;
    width: 80%;
    max-width: 500px;
}

.close-modal {
    color: #aaa;
    float: right;
    font-size: 28px;
    font-weight: bold;
    cursor: pointer;
}

.close-modal:hover {
    color: black;
}

.btn-secondary {
    padding: 8px 15px;
    background-color: #f0f0f0;
    border: 1px solid #ddd;
    border-radius: 5px;
    cursor: pointer;
    transition: background-color 0.3s;
}

.btn-secondary:hover {
    background-color: #e0e0e0;
}
</style>
`);