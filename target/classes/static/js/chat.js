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
                
                // 渲染聊天记录
                data.data.forEach(message => {
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
        const messageElement = document.createElement('div');
        messageElement.className = message.senderId == userId ? 'message sent' : 'message received';
        
        const messageContent = document.createElement('div');
        messageContent.className = 'message-content';
        messageContent.textContent = message.messageContent;
        
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
            messageType: 0 // 文本消息
        };
        
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
            if (data.code === 200) {
                // 清空输入框
                messageInput.value = '';
                
                // 添加消息到聊天区域
                const newMessage = {
                    senderId: userId,
                    messageContent: content
                };
                addMessageToChat(newMessage);
            } else {
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
        
        fetch(`/userContact/applyFriendAdd?token=${token}&contactId=${friendId}&applyInfo=${encodeURIComponent(applyMessage)}`, {
            method: 'POST'
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
    
    // 定时刷新联系人列表和当前聊天记录
    setInterval(function() {
        loadContacts();
        if (currentContactId) {
            loadChatHistory(currentContactId);
        }
    }, 10000); // 每10秒刷新一次
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