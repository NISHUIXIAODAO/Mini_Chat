import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiGet, apiPost, getErrorMessage, type LoginResult } from "@/lib/api";

type AuthMode = "login" | "register";

export default function AuthPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<AuthMode>("login");
  const [loading, setLoading] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [loginForm, setLoginForm] = useState({ email: "", password: "" });
  const [registerForm, setRegisterForm] = useState({
    email: "",
    nickName: "",
    password: "",
    confirmPassword: "",
    code: "",
    sex: "0",
  });

  useEffect(() => {
    if (cooldown <= 0) {
      return;
    }
    const timer = window.setTimeout(() => setCooldown((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [cooldown]);

  async function handleLogin(event: React.FormEvent) {
    event.preventDefault();
    if (!loginForm.email || !loginForm.password) {
      alert("请填写所有必填字段");
      return;
    }

    setLoading(true);
    try {
      const response = await apiPost<LoginResult>("/userInfo/login", loginForm);
      if (response.code === 200 && response.data) {
        localStorage.setItem("token", response.data.token);
        localStorage.setItem("userId", String(response.data.userId));
        localStorage.setItem("nickName", response.data.nickName || "用户");
        navigate("/chat");
        return;
      }
      alert(`登录失败: ${getErrorMessage(response)}`);
    } catch (error) {
      console.error("登录请求出错:", error);
      alert("登录请求出错，请稍后再试");
    } finally {
      setLoading(false);
    }
  }

  async function handleSendCode() {
    if (cooldown > 0) {
      return;
    }
    if (!registerForm.email) {
      alert("请先填写邮箱");
      return;
    }

    try {
      const response = await apiGet(`/userInfo/sendCode?email=${encodeURIComponent(registerForm.email)}`);
      if (response.code === 200) {
        setCooldown(60);
        alert("验证码已发送到您的邮箱");
        return;
      }
      alert(`验证码发送失败: ${getErrorMessage(response)}`);
    } catch (error) {
      console.error("验证码请求出错:", error);
      alert("验证码请求出错，请稍后再试");
    }
  }

  async function handleRegister(event: React.FormEvent) {
    event.preventDefault();
    const { email, nickName, password, confirmPassword, code, sex } = registerForm;
    if (!email || !nickName || !password || !confirmPassword || !code) {
      alert("请填写所有必填字段");
      return;
    }
    if (password !== confirmPassword) {
      alert("两次输入的密码不一致");
      return;
    }

    setLoading(true);
    try {
      const response = await apiPost("/userInfo/register", {
        email,
        nickName,
        password,
        code,
        sex: sex === "1",
      });
      if (response.code === 200) {
        alert("注册成功，请登录");
        setMode("login");
        return;
      }
      alert(`注册失败: ${getErrorMessage(response)}`);
    } catch (error) {
      console.error("注册请求出错:", error);
      alert("注册请求出错，请稍后再试");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="form-container">
        <div className="form-header">
          <h1>EasyChat</h1>
          <p>简单、高效的聊天应用</p>
        </div>

        <div className="form-tabs">
          <button className={`tab-btn ${mode === "login" ? "active" : ""}`} onClick={() => setMode("login")} type="button">
            登录
          </button>
          <button className={`tab-btn ${mode === "register" ? "active" : ""}`} onClick={() => setMode("register")} type="button">
            注册
          </button>
        </div>

        {mode === "login" ? (
          <form className="active-form" onSubmit={handleLogin}>
            <div className="form-group">
              <label htmlFor="login-email">邮箱</label>
              <input id="login-email" type="email" value={loginForm.email} onChange={(event) => setLoginForm({ ...loginForm, email: event.target.value })} />
            </div>
            <div className="form-group">
              <label htmlFor="login-password">密码</label>
              <input id="login-password" type="password" value={loginForm.password} onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })} />
            </div>
            <button className="btn-primary" disabled={loading} type="submit">
              {loading ? "登录中..." : "登录"}
            </button>
          </form>
        ) : (
          <form className="active-form" onSubmit={handleRegister}>
            <div className="form-group">
              <label htmlFor="register-email">邮箱</label>
              <input id="register-email" type="email" value={registerForm.email} onChange={(event) => setRegisterForm({ ...registerForm, email: event.target.value })} />
            </div>
            <div className="form-group">
              <label htmlFor="register-nickname">昵称</label>
              <input id="register-nickname" type="text" value={registerForm.nickName} onChange={(event) => setRegisterForm({ ...registerForm, nickName: event.target.value })} />
            </div>
            <div className="form-group">
              <label htmlFor="register-password">密码</label>
              <input id="register-password" type="password" value={registerForm.password} onChange={(event) => setRegisterForm({ ...registerForm, password: event.target.value })} />
            </div>
            <div className="form-group">
              <label htmlFor="register-confirm-password">确认密码</label>
              <input id="register-confirm-password" type="password" value={registerForm.confirmPassword} onChange={(event) => setRegisterForm({ ...registerForm, confirmPassword: event.target.value })} />
            </div>
            <div className="form-group">
              <label htmlFor="register-code">验证码</label>
              <div className="code-input-group">
                <input id="register-code" type="text" value={registerForm.code} onChange={(event) => setRegisterForm({ ...registerForm, code: event.target.value })} />
                <button className="btn-secondary" disabled={cooldown > 0} onClick={handleSendCode} type="button">
                  {cooldown > 0 ? `${cooldown}秒后重试` : "获取验证码"}
                </button>
              </div>
            </div>
            <div className="form-group">
              <label>性别</label>
              <div className="radio-group">
                <label>
                  <input checked={registerForm.sex === "0"} name="sex" onChange={() => setRegisterForm({ ...registerForm, sex: "0" })} type="radio" />
                  女
                </label>
                <label>
                  <input checked={registerForm.sex === "1"} name="sex" onChange={() => setRegisterForm({ ...registerForm, sex: "1" })} type="radio" />
                  男
                </label>
              </div>
            </div>
            <button className="btn-primary" disabled={loading} type="submit">
              {loading ? "注册中..." : "注册"}
            </button>
          </form>
        )}
      </section>
    </main>
  );
}
