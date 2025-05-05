// WebSocket连接管理
class WebSocketManager {
    constructor(userId) {
        this.userId = userId;
        this.socket = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectInterval = 3000; // 3秒
        this.messageCallbacks = [];
        this.connectionCallbacks = [];
    }

    // 连接WebSocket
    connect() {
        // 获取WebSocket服务器地址（从应用配置中获取端口）
        const wsPort = 5051; // 从application.properties中获取的ws.port值
        const wsUrl = `ws://${window.location.hostname}:${wsPort}/ws/${this.userId}`;
        
        this.socket = new WebSocket(wsUrl);
        
        this.socket.onopen = () => {
            console.log('WebSocket连接已建立');
            this.reconnectAttempts = 0;
            // 触发连接回调
            this.connectionCallbacks.forEach(callback => callback(true));
        };
        
        this.socket.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                console.log('收到消息:', message);
                // 触发消息回调
                this.messageCallbacks.forEach(callback => callback(message));
            } catch (error) {
                console.error('解析消息出错:', error);
            }
        };
        
        this.socket.onclose = () => {
            console.log('WebSocket连接已关闭');
            // 触发连接回调
            this.connectionCallbacks.forEach(callback => callback(false));
            
            // 尝试重新连接
            this.attemptReconnect();
        };
        
        this.socket.onerror = (error) => {
            console.error('WebSocket错误:', error);
        };
    }
    
    // 尝试重新连接
    attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            
            setTimeout(() => {
                this.connect();
            }, this.reconnectInterval);
        } else {
            console.error('达到最大重连次数，请刷新页面重试');
        }
    }
    
    // 发送消息
    sendMessage(message) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify(message));
            return true;
        } else {
            console.error('WebSocket未连接，无法发送消息');
            return false;
        }
    }
    
    // 添加消息回调
    onMessage(callback) {
        if (typeof callback === 'function') {
            this.messageCallbacks.push(callback);
        }
    }
    
    // 添加连接状态回调
    onConnectionChange(callback) {
        if (typeof callback === 'function') {
            this.connectionCallbacks.push(callback);
        }
    }
    
    // 关闭连接
    disconnect() {
        if (this.socket) {
            this.socket.close();
        }
    }
}

// 在聊天页面中使用WebSocketManager
document.addEventListener('DOMContentLoaded', function() {
    // 检查是否在聊天页面
    if (window.location.pathname.includes('chat.html')) {
        const userId = localStorage.getItem('userId');
        
        if (userId) {
            // 创建WebSocket管理器
            const wsManager = new WebSocketManager(userId);
            
            // 连接WebSocket
            wsManager.connect();
            
            // 监听消息
            wsManager.onMessage(message => {
                // 处理接收到的消息
                if (message.type === 'chat_message') {
                    // 如果当前正在与发送者聊天，则直接显示消息
                    const currentContactId = document.querySelector('.contact-item.active')?.dataset.contactId;
                    
                    if (currentContactId && (message.senderId == currentContactId || message.receiverId == currentContactId)) {
                        // 添加消息到聊天区域
                        const messageElement = document.createElement('div');
                        messageElement.className = message.senderId == userId ? 'message sent' : 'message received';
                        
                        const messageContent = document.createElement('div');
                        messageContent.className = 'message-content';
                        messageContent.textContent = message.content;
                        
                        messageElement.appendChild(messageContent);
                        document.getElementById('chat-messages').appendChild(messageElement);
                        
                        // 滚动到最新消息
                        document.getElementById('chat-messages').scrollTop = document.getElementById('chat-messages').scrollHeight;
                    }
                    
                    // 刷新联系人列表以更新最新消息
                    if (typeof loadContacts === 'function') {
                        loadContacts();
                    }
                }
            });
            
            // 监听连接状态变化
            wsManager.onConnectionChange(connected => {
                if (connected) {
                    console.log('已连接到聊天服务器');
                } else {
                    console.log('与聊天服务器的连接已断开');
                }
            });
            
            // 将WebSocket管理器添加到window对象，以便在其他脚本中使用
            window.wsManager = wsManager;
        }
    }
});