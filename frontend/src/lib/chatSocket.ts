import type { ChatMessage } from "./api";

type Callback<T> = (data: T) => void;

export class ChatSocket {
  private socket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private heartbeatTimer: number | null = null;
  private readonly maxReconnectAttempts = 5;
  private readonly reconnectInterval = 3000;
  private readonly heartbeatInterval = 30000;

  private chatCallbacks: Callback<ChatMessage>[] = [];
  private noticeCallbacks: Callback<ChatMessage>[] = [];
  private friendCallbacks: Callback<ChatMessage>[] = [];
  private statusCallbacks: Callback<boolean>[] = [];
  private forceOfflineCallbacks: Callback<ChatMessage>[] = [];
  private manualClose = false;

  constructor(
    private readonly token: string,
    private readonly userId: string,
  ) {}

  connect() {
    if (this.socket?.readyState === WebSocket.OPEN) {
      return;
    }

    this.manualClose = false;
    const wsUrl = `ws://${window.location.hostname}:5051/ws?token=${encodeURIComponent(this.token)}`;
    this.socket = new WebSocket(wsUrl);
    this.socket.onopen = () => this.handleOpen();
    this.socket.onmessage = (event) => this.handleMessage(event);
    this.socket.onclose = () => this.handleClose();
    this.socket.onerror = () => this.emitStatus(false);
  }

  disconnect() {
    this.manualClose = true;
    this.stopHeartbeat();
    this.socket?.close();
    this.socket = null;
  }

  isConnected() {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  sendMessage(message: ChatMessage) {
    if (!this.isConnected()) {
      return false;
    }
    this.socket?.send(
      JSON.stringify({
        ...message,
        sendUserId: message.sendUserId || this.userId,
        sendTime: message.sendTime || Date.now(),
      }),
    );
    return true;
  }

  onChatMessage(callback: Callback<ChatMessage>) {
    this.chatCallbacks.push(callback);
  }

  onSystemNotice(callback: Callback<ChatMessage>) {
    this.noticeCallbacks.push(callback);
  }

  onFriendRequest(callback: Callback<ChatMessage>) {
    this.friendCallbacks.push(callback);
  }

  onStatus(callback: Callback<boolean>) {
    this.statusCallbacks.push(callback);
  }

  onForceOffline(callback: Callback<ChatMessage>) {
    this.forceOfflineCallbacks.push(callback);
  }

  private handleOpen() {
    this.reconnectAttempts = 0;
    this.emitStatus(true);
    this.startHeartbeat();
  }

  private handleMessage(event: MessageEvent) {
    const message = JSON.parse(event.data) as ChatMessage;
    switch (message.messageType) {
      case 1:
        this.noticeCallbacks.forEach((callback) => callback(message));
        break;
      case 2:
        this.chatCallbacks.forEach((callback) => callback(message));
        break;
      case 3:
        this.friendCallbacks.forEach((callback) => callback(message));
        break;
      case 7:
        this.manualClose = true;
        this.stopHeartbeat();
        this.forceOfflineCallbacks.forEach((callback) => callback(message));
        this.socket?.close();
        break;
      default:
        this.chatCallbacks.forEach((callback) => callback(message));
    }
  }

  private handleClose() {
    this.emitStatus(false);
    this.stopHeartbeat();
    if (this.manualClose) {
      return;
    }
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      return;
    }
    this.reconnectAttempts += 1;
    window.setTimeout(() => this.connect(), this.reconnectInterval);
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatTimer = window.setInterval(() => {
      if (this.isConnected()) {
        this.socket?.send(JSON.stringify({ type: "heartbeat", timestamp: Date.now() }));
      }
    }, this.heartbeatInterval);
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      window.clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private emitStatus(connected: boolean) {
    this.statusCallbacks.forEach((callback) => callback(connected));
  }
}
