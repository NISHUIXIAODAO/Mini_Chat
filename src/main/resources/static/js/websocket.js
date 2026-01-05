// WebSocket连接管理
class WebSocketManager {
    constructor() {
        this.socket = null;
        this.userId = null;
        this.token = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectInterval = 3000; // 3秒
        this.heartbeatInterval = 30000; // 30秒发送一次心跳
        this.heartbeatTimer = null;
        this.connectionStatus = false;
        
        // 回调函数注册
        this.callbacks = {
            message: [], // 消息回调
            connection: [], // 连接状态回调
            error: [], // 错误回调
            // 特定消息类型的回调
            chatMessage: [], // 聊天消息
            systemNotice: [], // 系统通知
            friendRequest: [] // 好友请求
        };
    }

    // 初始化WebSocket连接
    init(token, userId) {
        this.token = token;
        this.userId = userId;
        this.connect();
        return this;
    }

    // 连接WebSocket
    connect() {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            console.log('WebSocket已连接，无需重新连接');
            return;
        }

        // 获取WebSocket服务器地址
        const wsPort = 5051; // 从application.properties中获取的ws.port值
        const wsUrl = `ws://${window.location.hostname}:${wsPort}/ws?token=${this.token}`;
        
        try {
            this.socket = new WebSocket(wsUrl);
            
            this.socket.onopen = this._handleOpen.bind(this);
            this.socket.onmessage = this._handleMessage.bind(this);
            this.socket.onclose = this._handleClose.bind(this);
            this.socket.onerror = this._handleError.bind(this);
        } catch (error) {
            console.error('创建WebSocket连接失败:', error);
            this._triggerCallbacks('error', error);
            this._attemptReconnect();
        }
    }
    
    // 处理连接打开事件
    _handleOpen() {
        console.log('WebSocket连接已建立');
        this.reconnectAttempts = 0;
        this.connectionStatus = true;
        this._triggerCallbacks('connection', true);
        
        // 启动心跳检测
        this._startHeartbeat();
    }
    
    // 处理接收消息事件
    _handleMessage(event) {
        try {
            const message = JSON.parse(event.data);
            console.log('收到消息:', message);
            
            // 触发通用消息回调
            this._triggerCallbacks('message', message);
            
            // 根据消息类型触发特定回调
            this._handleMessageByType(message);
        } catch (error) {
            console.error('解析消息出错:', error);
            this._triggerCallbacks('error', {
                type: 'parse_error',
                error: error,
                rawData: event.data
            });
        }
    }
    
    // 根据消息类型处理消息
    _handleMessageByType(message) {
        // 根据消息类型字段分发到不同的处理函数
        if (!message.messageType) {
            return;
        }
        
        switch (message.messageType) {
            case 1: // 系统通知
                this._triggerCallbacks('systemNotice', message);
                break;
            case 2: // 聊天消息
                this._triggerCallbacks('chatMessage', message);
                break;
            case 3: // 好友请求
                this._triggerCallbacks('friendRequest', message);
                break;
            default:
                console.log('未知消息类型:', message.messageType);
        }
    }
    
    // 处理连接关闭事件
    _handleClose(event) {
        console.log('WebSocket连接已关闭', event);
        this.connectionStatus = false;
        this._triggerCallbacks('connection', false);
        
        // 清除心跳定时器
        this._stopHeartbeat();
        
        // 尝试重新连接
        this._attemptReconnect();
    }
    
    // 处理错误事件
    _handleError(error) {
        console.error('WebSocket错误:', error);
        this._triggerCallbacks('error', error);
    }
    
    // 尝试重新连接
    _attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            
            setTimeout(() => {
                this.connect();
            }, this.reconnectInterval);
        } else {
            console.error('达到最大重连次数，请刷新页面重试');
            this._triggerCallbacks('error', {
                type: 'max_reconnect_attempts',
                message: '达到最大重连次数，请刷新页面重试'
            });
        }
    }
    
    // 启动心跳检测
    _startHeartbeat() {
        this._stopHeartbeat(); // 先清除可能存在的定时器
        
        this.heartbeatTimer = setInterval(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                // 发送心跳消息
                this.socket.send(JSON.stringify({
                    type: 'heartbeat',
                    timestamp: Date.now()
                }));
            }
        }, this.heartbeatInterval);
    }
    
    // 停止心跳检测
    _stopHeartbeat() {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }
    
    // 触发回调函数
    _triggerCallbacks(type, data) {
        if (this.callbacks[type]) {
            this.callbacks[type].forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error(`执行${type}回调出错:`, error);
                }
            });
        }
    }
    
    // 发送消息
    sendMessage(message) {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
            console.error('WebSocket未连接，无法发送消息');
            return false;
        }
        
        try {
            // 确保消息是对象
            const messageObj = typeof message === 'string' ? JSON.parse(message) : message;
            
            // 添加发送时间戳
            if (!messageObj.sendTime) {
                messageObj.sendTime = Date.now();
            }
            
            // 添加发送者ID
            if (!messageObj.sendUserId && this.userId) {
                messageObj.sendUserId = this.userId;
            }
            
            this.socket.send(JSON.stringify(messageObj));
            return true;
        } catch (error) {
            console.error('发送消息失败:', error);
            this._triggerCallbacks('error', {
                type: 'send_error',
                error: error,
                message: message
            });
            return false;
        }
    }
    
    // 注册消息回调
    onMessage(callback) {
        this._registerCallback('message', callback);
        return this;
    }
    
    // 注册连接状态回调
    onConnectionChange(callback) {
        this._registerCallback('connection', callback);
        
        // 如果已经连接，立即触发回调
        if (this.connectionStatus) {
            try {
                callback(true);
            } catch (error) {
                console.error('执行连接状态回调出错:', error);
            }
        }
        
        return this;
    }
    
    // 注册错误回调
    onError(callback) {
        this._registerCallback('error', callback);
        return this;
    }
    
    // 注册聊天消息回调
    onChatMessage(callback) {
        this._registerCallback('chatMessage', callback);
        return this;
    }
    
    // 注册系统通知回调
    onSystemNotice(callback) {
        this._registerCallback('systemNotice', callback);
        return this;
    }
    
    // 注册好友请求回调
    onFriendRequest(callback) {
        this._registerCallback('friendRequest', callback);
        return this;
    }
    
    // 注册回调函数
    _registerCallback(type, callback) {
        if (typeof callback === 'function') {
            if (!this.callbacks[type]) {
                this.callbacks[type] = [];
            }
            this.callbacks[type].push(callback);
        }
    }
    
    // 获取连接状态
    isConnected() {
        return this.socket && this.socket.readyState === WebSocket.OPEN;
    }
    
    // 关闭连接
    disconnect() {
        this._stopHeartbeat();
        
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
        
        this.connectionStatus = false;
    }
}

// 创建全局WebSocket实例
let wsInstance = null;

// 获取WebSocket实例的函数
function getWebSocketInstance() {
    if (!wsInstance) {
        wsInstance = new WebSocketManager();
    }
    return wsInstance;
}

// 在页面加载时初始化WebSocket
document.addEventListener('DOMContentLoaded', function() {
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    
    if (token && userId) {
        // 初始化WebSocket
        const ws = getWebSocketInstance().init(token, userId);
        
        // 监听连接状态
        ws.onConnectionChange(connected => {
            if (connected) {
                console.log('已连接到聊天服务器');
            } else {
                console.log('与聊天服务器的连接已断开');
            }
        });
        
        // 监听错误
        ws.onError(error => {
            console.error('WebSocket错误:', error);
        });
        
        // 如果在聊天页面，设置消息处理
        if (window.location.pathname.includes('chat.html')) {
            setupChatHandlers(ws);
        }
    }
});

// 设置聊天页面的消息处理
function setupChatHandlers(ws) {
    const userId = localStorage.getItem('userId');
    
    // 处理聊天消息
    ws.onChatMessage(message => {
        // 如果当前正在与发送者聊天，则直接显示消息
        const currentContactId = document.querySelector('.contact-item.active')?.dataset.contactId;
        
        if (currentContactId && (message.sendUserId == currentContactId || message.contactId == currentContactId)) {
            // 添加消息到聊天区域
            const messageElement = document.createElement('div');
            messageElement.className = message.sendUserId == userId ? 'message sent' : 'message received';
            
            const messageContent = document.createElement('div');
            messageContent.className = 'message-content';
            messageContent.textContent = message.messageContent;
            
            messageElement.appendChild(messageContent);
            document.getElementById('chat-messages').appendChild(messageElement);
            
            // 滚动到最新消息
            document.getElementById('chat-messages').scrollTop = document.getElementById('chat-messages').scrollHeight;
        }
        
        // 刷新联系人列表以更新最新消息
        if (typeof loadContacts === 'function') {
            loadContacts();
        }
    });
    
    // 处理好友请求
    ws.onFriendRequest(message => {
        // 显示好友请求通知
        alert(`收到来自 ${message.sendUserNickName} 的好友请求: ${message.messageContent}`);
        
        // 刷新联系人列表
        if (typeof loadContacts === 'function') {
            loadContacts();
        }
    });
    
    // 处理系统通知
    ws.onSystemNotice(message => {
        // 显示系统通知
        alert(`系统通知: ${message.messageContent}`);
    });
}

// 导出WebSocket实例获取函数
window.getWebSocketInstance = getWebSocketInstance;