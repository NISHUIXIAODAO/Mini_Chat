import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiGet, apiPost, getErrorMessage, getToken, type ChatMessage, type Contact } from "@/lib/api";
import { ChatSocket } from "@/lib/chatSocket";

export default function ChatPage() {
  const navigate = useNavigate();
  const messagesRef = useRef<HTMLDivElement | null>(null);
  const socketRef = useRef<ChatSocket | null>(null);
  const selectedContactRef = useRef<Contact | null>(null);
  const loadContactsRef = useRef<() => Promise<void>>();
  const incomingMessageRef = useRef<(message: ChatMessage) => void>();
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [selectedContact, setSelectedContact] = useState<Contact | null>(null);
  const [messageText, setMessageText] = useState("");
  const [friendId, setFriendId] = useState("");
  const [applyInfo, setApplyInfo] = useState("请求添加您为好友");
  const [modalOpen, setModalOpen] = useState(false);
  const [connected, setConnected] = useState(false);

  const token = getToken();
  const userId = localStorage.getItem("userId") || "";
  const nickName = localStorage.getItem("nickName") || "用户";

  const userInitial = useMemo(() => nickName.charAt(0).toUpperCase() || "U", [nickName]);

  useEffect(() => {
    selectedContactRef.current = selectedContact;
  }, [selectedContact]);

  useEffect(() => {
    messagesRef.current?.scrollTo({ top: messagesRef.current.scrollHeight });
  }, [messages]);

  const clearLocalSession = useCallback(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("nickName");
    socketRef.current?.disconnect();
    navigate("/");
  }, [navigate]);

  const handleLogout = useCallback(async () => {
    if (token) {
      await apiPost("/userInfo/logout");
    }
    clearLocalSession();
  }, [clearLocalSession, token]);

  const loadContacts = useCallback(async () => {
    const response = await apiGet<Contact[]>("/userContact/getContactList");
    if (response.code === 401) {
      handleLogout();
      return;
    }
    if (response.code === 200 && response.data) {
      setContacts(response.data);
      return;
    }
    console.error("加载联系人失败:", getErrorMessage(response));
  }, [handleLogout]);

  async function loadChatHistory(contact: Contact) {
    setSelectedContact(contact);
    const response = await apiGet<ChatMessage[]>(`/chat/getMessageHistory?contactId=${contact.contactId}`);
    if (response.code === 401) {
      handleLogout();
      return;
    }
    if (response.code === 200 && response.data) {
      const sorted = [...response.data].sort((a, b) => Number(a.sendTime || 0) - Number(b.sendTime || 0));
      setMessages(sorted);
      return;
    }
    console.error("加载聊天记录失败:", getErrorMessage(response));
  }

  const handleIncomingMessage = useCallback((message: ChatMessage) => {
    if (!message?.messageContent) {
      return;
    }
    const senderId = String(message.senderId || message.sendUserId || "");
    const contactId = String(message.contactId || "");
    const selectedId = String(selectedContactRef.current?.contactId || "");

    if (selectedId && (senderId === selectedId || contactId === selectedId)) {
      setMessages((current) => {
        const duplicated = current.some((item) => isSameMessage(item, message));
        return duplicated ? current : [...current, message];
      });
    }

    const targetContactId = senderId === String(userId) ? contactId : senderId;
    updateContactPreview(targetContactId, message.messageContent);
    loadContacts();
  }, [loadContacts, userId]);

  useEffect(() => {
    loadContactsRef.current = loadContacts;
    incomingMessageRef.current = handleIncomingMessage;
  }, [handleIncomingMessage, loadContacts]);

  useEffect(() => {
    if (!token || !userId) {
      navigate("/");
      return;
    }

    loadContactsRef.current?.();
    const socket = new ChatSocket(token, userId);
    socketRef.current = socket;
    socket.onStatus(setConnected);
    socket.onChatMessage((message) => incomingMessageRef.current?.(message));
    socket.onSystemNotice((message) => {
      alert(`系统通知: ${message.messageContent}`);
      loadContactsRef.current?.();
    });
    socket.onFriendRequest((message) => {
      alert(`收到来自 ${message.sendUserNickName || "用户"} 的好友请求: ${message.messageContent}`);
      loadContactsRef.current?.();
    });
    socket.onForceOffline((message) => {
      alert(message.messageContent || "账号已在其他设备登录");
      clearLocalSession();
    });
    socket.connect();

    return () => socket.disconnect();
  }, [clearLocalSession, navigate, token, userId]);

  function updateContactPreview(contactId: string, content: string) {
    setContacts((current) => {
      const index = current.findIndex((contact) => String(contact.contactId) === contactId);
      if (index < 0) {
        return current;
      }
      const next = [...current];
      const [contact] = next.splice(index, 1);
      next.unshift({ ...contact, lastMessage: content });
      return next;
    });
  }

  function isSameMessage(left: ChatMessage, right: ChatMessage) {
    if (left.messageId && right.messageId) {
      return left.messageId === right.messageId;
    }

    const leftTime = String(left.sendTime || "");
    const rightTime = String(right.sendTime || "");
    if (!leftTime || !rightTime) {
      return false;
    }

    const leftSender = String(left.senderId || left.sendUserId || "");
    const rightSender = String(right.senderId || right.sendUserId || "");
    const leftContact = String(left.contactId || "");
    const rightContact = String(right.contactId || "");

    return (
      leftTime === rightTime &&
      leftSender === rightSender &&
      leftContact === rightContact &&
      left.messageContent === right.messageContent
    );
  }

  async function sendMessage() {
    const content = messageText.trim();
    if (!content || !selectedContact) {
      return;
    }

    const payload: ChatMessage = {
      contactId: selectedContact.contactId,
      messageContent: content,
      messageType: 2,
      sendUserId: userId,
    };

    setMessages((current) => [...current, payload]);
    setMessageText("");
    socketRef.current?.sendMessage(payload);

    const response = await apiPost("/chat/sendMessage", payload);
    if (response.code !== 200) {
      alert(`发送消息失败: ${getErrorMessage(response, `未知错误，状态码:${response.code}`)}`);
    }
  }

  async function submitFriendRequest() {
    if (!friendId) {
      alert("请输入好友ID");
      return;
    }
    const response = await apiPost(`/userContact/applyFriendAdd?contactId=${friendId}&applyInfo=${encodeURIComponent(applyInfo || "请求添加您为好友")}`);
    if (response.code === 200) {
      alert("好友申请已发送");
      setModalOpen(false);
      setFriendId("");
      setApplyInfo("请求添加您为好友");
      return;
    }
    alert(`发送好友申请失败: ${getErrorMessage(response)}`);
  }

  return (
    <main className="chat-container">
      <aside className="sidebar">
        <div className="user-info">
          <div className="user-avatar">{userInitial}</div>
          <div className="user-meta">
            <div className="user-name">{nickName}</div>
            <div className={`connection-status ${connected ? "online" : ""}`}>{connected ? "实时在线" : "连接中"}</div>
          </div>
          <button className="logout-button" onClick={handleLogout} type="button">
            退出
          </button>
        </div>
        <div className="search-box">
          <input className="search-input" placeholder="搜索联系人..." type="text" />
        </div>
        <div className="contact-list">
          {contacts.length === 0 ? (
            <div className="empty-state">暂无联系人</div>
          ) : (
            contacts.map((contact) => (
              <button
                className={`contact-item ${selectedContact?.contactId === contact.contactId ? "active" : ""}`}
                key={contact.contactId}
                onClick={() => loadChatHistory(contact)}
                type="button"
              >
                <span className="contact-avatar">{contact.nickName?.charAt(0).toUpperCase() || "U"}</span>
                <span className="contact-info">
                  <span className="contact-name">{contact.nickName}</span>
                  <span className="contact-last-message">{contact.lastMessage || "暂无消息"}</span>
                </span>
              </button>
            ))
          )}
        </div>
      </aside>

      <section className="chat-main">
        <header className="chat-header">
          <div className="chat-title">{selectedContact?.nickName || "选择一个联系人开始聊天"}</div>
          <div className="chat-actions">
            <button className="btn-secondary" onClick={() => setModalOpen(true)} type="button">
              添加好友
            </button>
          </div>
        </header>
        <div className="chat-messages" ref={messagesRef}>
          {!selectedContact ? (
            <div className="empty-chat">从左侧选择联系人后开始聊天</div>
          ) : messages.length === 0 ? (
            <div className="empty-chat">暂无聊天记录</div>
          ) : (
            messages.map((message, index) => {
              const senderId = String(message.senderId || message.sendUserId || "");
              const isSent = senderId === String(userId);
              return (
                <div className={`message ${isSent ? "sent" : "received"}`} key={`${message.messageId || index}-${message.sendTime || index}`}>
                  <div className="message-content">{message.messageContent}</div>
                </div>
              );
            })
          )}
        </div>
        <div className="chat-input">
          <input
            className="message-input"
            disabled={!selectedContact}
            onChange={(event) => setMessageText(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                sendMessage();
              }
            }}
            placeholder="输入消息..."
            type="text"
            value={messageText}
          />
          <button className="send-button" disabled={!selectedContact || !messageText.trim()} onClick={sendMessage} type="button">
            发送
          </button>
        </div>
      </section>

      {modalOpen && (
        <div className="modal" onMouseDown={() => setModalOpen(false)}>
          <div className="modal-content" onMouseDown={(event) => event.stopPropagation()}>
            <button className="close-modal" onClick={() => setModalOpen(false)} type="button">
              &times;
            </button>
            <h2>添加好友</h2>
            <div className="form-group">
              <label htmlFor="friend-id">好友ID</label>
              <input id="friend-id" type="number" value={friendId} onChange={(event) => setFriendId(event.target.value)} />
            </div>
            <div className="form-group">
              <label htmlFor="apply-message">申请信息</label>
              <input id="apply-message" type="text" value={applyInfo} onChange={(event) => setApplyInfo(event.target.value)} />
            </div>
            <button className="btn-primary" onClick={submitFriendRequest} type="button">
              发送申请
            </button>
          </div>
        </div>
      )}
    </main>
  );
}
